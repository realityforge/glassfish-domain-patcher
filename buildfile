require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/custom_pom'

desc 'GlassFish Domain Patcher'
define 'glassfish-domain-patcher' do
  project.group = 'org.realityforge.glassfish.patcher'
  compile.options.source = '1.7'
  compile.options.target = '1.7'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/glassfish-domain-patcher')
  pom.add_developer('realityforge', 'Peter Donald', 'peter@realityforge.org', ['Developer'])

  package(:jar)
  package(:sources)
  package(:javadoc)
end

