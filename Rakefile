# frozen_string_literal: true

require 'date'
require 'rake/clean'

CLOBBER << '_site'
CLOBBER << '_posts'

desc 'Install dependencies'
task :init do
  sh 'bundle', 'install'
end

desc 'Update dependencies'
task :update do
  sh 'bundle', 'update'
end

desc 'Build posts'
task :posts do
  sh './sbt', 'run'
end

desc 'Build the site'
task :build => [:init, :posts] do
  sh 'bundle', 'exec', 'jekyll', 'build'
end

desc 'Continually build posts'
task :watch_posts do
  sh './sbt', '~ run'
end

desc 'Continually serve the site'
task :serve => [:init] do
  sh 'bundle', 'exec', 'jekyll', 'serve', '--watch', '--baseurl', ''
end

desc 'Build posts and serve in parallel (for development)'
multitask :dev => [:serve, :watch_posts]
