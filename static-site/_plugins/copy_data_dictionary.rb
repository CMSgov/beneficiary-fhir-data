require 'fileutils'

# TODO: may have to muck with the file paths for CI but this works running locally for now

Jekyll::Hooks.register :site, :after_init do ||
   copy_data_dictionary("1")
   copy_data_dictionary("2")
  end

def copy_data_dictionary(version)
    current_dir = File.dirname(__FILE__)
    # find the latest copy of the data dictionary (sorted by filename)
    dd_source = Dir[File.join(current_dir,"../../dist/V#{version}-data-dictionary*json")].sort[-1]
    contents = File.read(dd_source)
    # un-escape extra newlines in the example json so it renders properly on the site
    replaced = contents.gsub(/\\\\n/, "\\n")
    data_folder = File.join(current_dir, "../_data");
    FileUtils.mkdir_p data_folder
    dd_dest = File.join(data_folder, "/data-dictionary-v#{version}.json")
    # write the modified contents to the _data folder
    File.open(dd_dest, "w") { |file| file.puts replaced }
end
