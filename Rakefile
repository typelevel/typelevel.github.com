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
task :build => [:posts] do
  sh 'bundle', 'exec', 'jekyll', 'build'
end
