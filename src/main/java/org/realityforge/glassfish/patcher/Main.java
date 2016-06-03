package org.realityforge.glassfish.patcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * A simple commandline application for patching domain.xml.
 */
public class Main
{
  private static final int HELP_OPT = 1;
  private static final int SYSTEM_SETTING_OPT = 's';
  private static final int VERBOSE_OPT = 'v';
  private static final int CONTENT_PATCH_OPT = 'p';
  private static final int FILE_OPT = 'f';
  private static final int OUTPUT_FILE_OPT = 'o';

  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]{
    new CLOptionDescriptor( "help",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            HELP_OPT,
                            "print this message and exit" ),
    new CLOptionDescriptor( "file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            FILE_OPT,
                            "the domain file to patch." ),
    new CLOptionDescriptor( "output-file",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            OUTPUT_FILE_OPT,
                            "the file to update with patched domain. Defaults to the file to patch if unspecified." ),
    new CLOptionDescriptor( "content-patch",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            CONTENT_PATCH_OPT,
                            "the domain file to patch." ),
    new CLOptionDescriptor( "system-setting",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            SYSTEM_SETTING_OPT,
                            "System setting to replace and the value to replace it with" ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "print verbose message while sending the message." )
  };

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 1;
  private static final int ERROR_PATCHING_CODE = 2;

  private static boolean c_verbose;
  private static final List<Patch> c_patches = new ArrayList<>();
  private static final List<SystemSetting> c_systemSettings = new ArrayList<>();
  private static File c_domainFile;
  private static File c_outputFile;

  public static void main( final String[] args )
  {
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    try
    {
      if ( c_verbose )
      {
        info( "Patching domain file " + c_domainFile.getAbsolutePath() );
      }
      final Document document = readDocument();

      patchElement( document.getDocumentElement() );
      updateSystemSettings( document );

      outputDocument( document );

      if ( c_verbose )
      {
        info( "Patched domain file wrote to " + c_outputFile.getAbsolutePath() );
        if ( !c_patches.isEmpty() )
        {
          info( "Content patch results:" );
          for ( final Patch patch : c_patches )
          {
            final String message =
              "\t" + patch.getKey() + " => " + patch.getValue() + " replaced " + patch.getCount() + " times(s)";
            info( message );
          }
        }
        if ( !c_systemSettings.isEmpty() )
        {
          info( "System Setting results:" );
          for ( final SystemSetting setting : c_systemSettings )
          {
            final String message =
              "\t" + setting.getKey() + " = " + setting.getValue() + " " + ( setting.isUpdated() ? "updated" : "set" );
            info( message );
          }
        }
      }
      System.exit( SUCCESS_EXIT_CODE );
    }
    catch ( final Exception e )
    {
      error( "Error patching domain file " + c_domainFile.getAbsolutePath() + ". Error: " + e );
      if ( c_verbose )
      {
        e.printStackTrace( System.out );
      }
      System.exit( ERROR_PATCHING_CODE );
    }
  }

  private static void updateSystemSettings( final Document document )
    throws Exception
  {
    final Element servers = (Element) document.getDocumentElement().getElementsByTagName( "servers" ).item( 0 );
    final Element server = (Element) servers.getElementsByTagName( "server" ).item( 0 );
    final XPath xpath = XPathFactory.newInstance().newXPath();
    for ( final SystemSetting setting : c_systemSettings )
    {

      final Element element =
        (Element) xpath.evaluate( "system-property[@name='" + setting.getKey() + "']", server, XPathConstants.NODE );
      if ( null != element )
      {
        element.setAttribute( "value", setting.getValue() );
        setting.setUpdated( true );
      }
      else
      {
        final Element e = document.createElement( "system-property" );
        e.setAttribute( "name", setting.getKey() );
        e.setAttribute( "value", setting.getValue() );
        server.appendChild( e );
      }
    }
  }

  private static void patchElement( final Element element )
  {
    patchAttributes( element );

    final NodeList nodeList = element.getChildNodes();
    final int length = nodeList.getLength();
    for ( int i = 0; i < length; i++ )
    {
      final Node item = nodeList.item( i );
      if ( item instanceof Element )
      {
        patchElement( (Element) item );
      }
      else if ( item instanceof Text || item instanceof Attr )
      {
        patchNode( item );
      }
    }
  }

  private static void patchAttributes( final Element element )
  {
    final NamedNodeMap attributes = element.getAttributes();
    if ( null != attributes )
    {
      final int length = attributes.getLength();
      for ( int i = 0; i < length; i++ )
      {
        patchNode( attributes.item( i ) );
      }
    }
  }

  private static void patchNode( final Node item )
  {
    final String textContent = item.getTextContent();
    if ( null != textContent )
    {
      for ( final Patch patch : c_patches )
      {
        final Pattern pattern = patch.getPattern();
        final String value = patch.getValue();
        final String newValue = pattern.matcher( textContent ).replaceAll( value );
        if ( !newValue.equals( textContent ) )
        {
          patch.incCount();
          item.setTextContent( newValue );
        }
      }
    }
  }

  private static Document readDocument()
    throws ParserConfigurationException, SAXException, IOException
  {
    final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    return db.parse( c_domainFile );
  }

  private static void outputDocument( final Document document )
    throws FileNotFoundException, TransformerException
  {
    final Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
    transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
    transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
    transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
    transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );
    final DOMSource source = new DOMSource( document );
    final StreamResult result = new StreamResult( new FileOutputStream( c_outputFile ) );
    transformer.transform( source, result );
  }

  private static boolean processOptions( final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if ( null != parser.getErrorString() )
    {
      error( parser.getErrorString() );
      return false;
    }

    // Get a list of parsed options
    final List<CLOption> options = parser.getArguments();
    for ( final CLOption option : options )
    {
      switch ( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
        {
          error( "Invalid text argument supplied: " + option.getArgument() );
          return false;
        }
        case CONTENT_PATCH_OPT:
        {
          final String key = option.getArgument();
          final String value = option.getArgument( 1 );
          final Pattern pattern = Pattern.compile( key, Pattern.LITERAL );
          c_patches.add( new Patch( key, value, pattern ) );
          break;
        }
        case SYSTEM_SETTING_OPT:
        {
          c_systemSettings.add( new SystemSetting( option.getArgument(), option.getArgument( 1 ) ) );
          break;
        }
        case OUTPUT_FILE_OPT:
        {
          c_outputFile = new File( option.getArgument() );
          break;
        }
        case FILE_OPT:
        {
          c_domainFile = new File( option.getArgument() );
          break;
        }
        case VERBOSE_OPT:
        {
          c_verbose = true;
          break;
        }
        case HELP_OPT:
        {
          printUsage();
          return false;
        }
      }
    }
    if ( null == c_domainFile )
    {
      error( "No domain file specified." );
      return false;
    }
    if ( !c_domainFile.exists() )
    {
      error( "Domain file specified " + c_domainFile.getAbsolutePath() + " does not exist." );
      return false;
    }
    if ( !c_domainFile.canRead() )
    {
      error( "Domain file specified " + c_domainFile.getAbsolutePath() + " is not readable." );
      return false;
    }
    if ( null == c_outputFile )
    {
      c_outputFile = c_domainFile;
    }
    if ( c_verbose )
    {
      info( "Domain file: " + c_domainFile.getAbsolutePath() );
      if ( !c_patches.isEmpty() )
      {
        info( "Content patches:" );
        for ( final Patch patch : c_patches )
        {
          info( "\t" + patch.getKey() + " => " + patch.getValue() );
        }
      }
      if ( !c_systemSettings.isEmpty() )
      {
        info( "System Setting patches:" );
        for ( final SystemSetting systemSetting : c_systemSettings )
        {
          info( "\t" + systemSetting.getKey() + " = " + systemSetting.getValue() );
        }
      }
    }

    return true;
  }

  /**
   * Print out a usage statement
   */
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );

    final StringBuilder msg = new StringBuilder();

    msg.append( "java " );
    msg.append( Main.class.getName() );
    msg.append( " [options] message" );
    msg.append( lineSeparator );
    msg.append( "Options: " );
    msg.append( lineSeparator );

    msg.append( CLUtil.describeOptions( OPTIONS ).toString() );

    info( msg.toString() );
  }

  private static void info( final String message )
  {
    System.out.println( message );
  }

  private static void error( final String message )
  {
    System.out.println( "Error: " + message );
  }
}
