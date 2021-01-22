require 'rake'
require 'io/console'

task :default => :unit_tests

desc "Run Android lint on all modules"
task :lint do
  sh "./gradlew clean lint :GooglePayment:assembleDebug :GooglePayment:assembleRelease"
end

desc "Run Android unit tests"
task :unit_tests => :lint do
  sh "./gradlew --continue test"
end

desc "Interactive release to publish new version to maven local"
task :release_local do
  sh "./gradlew clean publishToMavenLocal"
end

desc "Interactive release to publish new version"
task :release => :unit_tests do
  Rake::Task["assumptions"].invoke

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  update_version(version)
  update_readme_version(version)

  prompt_for_sonatype_username_and_password

  Rake::Task["release_braintree_google_payment"].invoke

  post_release(version)
end

task :assumptions do
    puts "Release Assumptions"
    puts "* [ ] You are on the branch and commit you want to release."
    puts "* [ ] You have already merged hotfixes and pulled changes."
    puts "* [ ] You have already reviewed the diff between the current release and the last tag, noting breaking changes in the semver and CHANGELOG."
    puts "* [ ] Tests (rake integration_tests) are passing, manual verifications complete."

    puts "Ready to release? Press any key to continue. "
    $stdin.gets
end

task :release_braintree_google_payment do
  sh "./gradlew clean :GooglePayment:publishToSonatype"
  sh "./gradlew closeAndReleaseRepository"
end

def prompt_for_sonatype_username_and_password
  puts "Enter Sonatype username:"
  ENV["SONATYPE_USERNAME"] = $stdin.gets.chomp

  puts "Enter Sonatype password:"
  ENV["SONATYPE_PASSWORD"] = $stdin.noecho(&:gets).chomp
end

def post_release(version)
  if !`git remote`.include?("github")
    sh "git remote add github https://github.com/braintree/braintree-android-google-payment.git"
  end

  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"
  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -a -m 'Release #{version}'"

  version_values = version.split('.')
  version_values[2] = version_values[2].to_i + 1
  update_version("#{version_values.join('.')}-SNAPSHOT")
  increment_version_code
  sh "git commit -am 'Prepare for development'"

  puts "\nDone. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin master #{version}"

  puts "\nPushed to GHE! Press ENTER to push to public Github."
  $stdin.gets
end

def get_current_version
  current_version = nil
  File.foreach("build.gradle") do |line|
    if match = line.match(/^version '(\d+\.\d+\.\d+(-SNAPSHOT)?)'/)
      current_version = match.captures
    end
  end

  return current_version[0]
end

def increment_version_code
  new_build_file = ""
  File.foreach("build.gradle") do |line|
    if line.match(/versionCode = (\d+)/)
      new_build_file += line.gsub(/versionCode = \d+/, "versionCode = #{$1.to_i + 1}")
    else
      new_build_file += line
    end
  end
  IO.write('build.gradle', new_build_file)
end

def update_version(version)
  IO.write("build.gradle",
    File.open("build.gradle") do |file|
      file.read.gsub(/^version '\d+\.\d+\.\d+(-SNAPSHOT)?'/, "version '#{version}'")
    end
  )
end

def update_readme_version(version)
  IO.write("README.md",
    File.open("README.md") do |file|
      file.read.gsub(/:google-payment:\d+\.\d+\.\d+'/, ":google-payment:#{version}'")
    end
  )
end

