require 'buildr/git_auto_version'
require 'buildr/gpg'

desc 'GlassFish Domain Patcher'
define 'glassfish-domain-patcher' do
  project.group = 'org.realityforge.glassfish.patcher'
  compile.options.source = '1.7'
  compile.options.target = '1.7'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/glassfish-domain-patcher')
  pom.add_developer('realityforge', 'Peter Donald')

  compile.with :getopt4j

  package(:jar).tap do |jar|
    jar.manifest['Main-Class'] = 'org.realityforge.glassfish.patcher.Main'
    jar.merge(:getopt4j)
  end
  package(:sources)
  package(:javadoc)
end
