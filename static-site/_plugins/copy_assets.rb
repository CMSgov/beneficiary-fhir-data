require 'fileutils'

Jekyll::Hooks.register :site, :after_init do ||
   copy_data_dictionary("1")
   copy_data_dictionary("2")
   copy_openapi()
  end

def copy_data_dictionary(version)
    contents = find_dist_file("../../dist/V#{version}-data-dictionary*json")
    # un-escape extra newlines in the example json so it renders properly on the site
    replaced = contents.gsub(/\\\\n/, "\\n")
    current_dir = File.dirname(__FILE__)
    data_folder = File.join(current_dir, "../_data");
    FileUtils.mkdir_p data_folder
    dd_dest = File.join(data_folder, "/data-dictionary-v#{version}.json")
    write_file(dd_dest, replaced)
end

def copy_openapi()
  contents = find_dist_file("../../dist/OpenAPI*yaml")
  current_dir = File.dirname(__FILE__)
  assets_folder = File.join(current_dir, "../assets");
  openapi_dest = File.join(assets_folder, "../assets/open-api.yaml")
  write_file(openapi_dest, contents)
end

def find_dist_file(relative_path)
  current_dir = File.dirname(__FILE__)
  # find the latest copy of the file (sorted by filename)
  dd_source = Dir[File.join(current_dir,relative_path)].sort[-1]
  return File.read(dd_source)
end

def write_file(dest_path, contents)
  File.open(dest_path, "w") { |file| file.puts contents }
end
