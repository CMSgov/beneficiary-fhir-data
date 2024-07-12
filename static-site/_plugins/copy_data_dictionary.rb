require 'fileutils'

# TODO: may have to muck with the file paths for CI but this works running locally for now

Jekyll::Hooks.register :site, :after_init do ||
   copy_data_dictionary("1")
   copy_data_dictionary("2")
  end

def copy_data_dictionary(version)
    path = ENV["BFD_PATH"]
    dd_source = Dir[File.join(path,"/dist/V#{version}-data-dictionary-*json")].sort[-1]
    contents = File.read(dd_source)
    replaced = contents.gsub(/\\\\n/, "\\n")
    data_folder = File.join(path, "/static-site/_data");
    FileUtils.mkdir_p data_folder
    dd_dest = File.join(data_folder, "/data-dictionary-v#{version}.json")
    File.open(dd_dest, "w") { |file| file.puts replaced }
end
