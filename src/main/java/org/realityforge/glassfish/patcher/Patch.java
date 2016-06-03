package org.realityforge.glassfish.patcher;

import java.util.regex.Pattern;

public class Patch
{
  private final String _key;
  private final String _value;
  private final Pattern _pattern;
  private int _count;

  public Patch( final String key, final String value, final Pattern pattern )
  {
    _key = key;
    _value = value;
    _pattern = pattern;
  }

  public String getKey()
  {
    return _key;
  }

  public String getValue()
  {
    return _value;
  }

  public Pattern getPattern()
  {
    return _pattern;
  }

  public int getCount()
  {
    return _count;
  }

  public void incCount()
  {
    _count++;
  }
}
