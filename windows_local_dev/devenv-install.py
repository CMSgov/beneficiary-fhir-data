#!/usr/bin/env python3

# This script downloads and installs Eclipse and other development tools from 
# the internet, into the user's '~/workspaces/tools' folder.
#
# It will do the following:
# * Create the `~/workspaces/tools` directory structure.
# * Download and install Eclipse (with required plugins).
# * Create a launcher shortcut for Eclipse.
# * Download and install Apache Maven.
#
# At this time, it is known to support the following platforms:
# * Windows, with Cygwin and the following packages installed:
#     * python3
#     * python3-lxml
#     * unzip
#     * cabextract
#
# Usage:
# This script is intended only for standalone usage, e.g.:
# $ ./devenv-install.py

from urllib.parse import urlsplit
import urllib.request
import ssl
import shutil
import cgi
import sys
import os
import fnmatch
import tarfile
import zipfile
import re
import collections
import subprocess
import tempfile
import stat
from pathlib import Path
import xml.etree.ElementTree
from lxml import etree

def main():
    """
    The main function for this script.
    """
    
    print('Development Environment Installer')
    print('=================================')
    
    # Verify that we're on a supported platform.
    # Reference: http://stackoverflow.com/questions/446209/possible-values-from-sys-platform
    if sys.platform == 'cygwin':
        #print('Supported platform: ' + sys.platform)
        pass
    else:
        raise OSError('Unsupported platform: ' + sys.platform)
    
    # Install JDK.
    jdk_archive_path = jdk_download()
    jdk_install_path = jdk_install(jdk_archive_path)
    jdk_config_env(jdk_install_path)
    
    # Install Eclipse.
    eclipse_archive_path = eclipse_download()
    eclipse_install_path = eclipse_install(eclipse_archive_path, jdk_install_path)
    eclipse_create_shortcut(eclipse_install_path)
    eclipse_install_plugins(eclipse_install_path)

    # Install Maven.
    maven_archive_path = maven_download()
    maven_install_path = maven_install(maven_archive_path)
    maven_config_env(maven_install_path, jdk_install_path)

    print('')
    print('Done.')

def jdk_download():
    """
    Download the JDK archive/installer from TODO.

    Returns:
        The path to the downloaded file, which will be saved into the
        `get_installers_dir()` directory.
    """
    
    # The URL to download from.
    jdk_urls = {
        'cygwin': 'http://download.oracle.com/otn-pub/java/jdk/8u172-b11/a58eab1ec242421181065cdc37240b08/jdk-8u172-windows-x64.exe'
    }
    jdk_url = jdk_urls[sys.platform]
    
    # The path to save the installer to.
    file_name = urlsplit(jdk_url).path.split('/')[-1]
    file_path_local = os.path.join(get_installers_dir(), file_name)
    
    print('* Install JDK')
    
    if not os.path.exists(file_path_local):
        # Download the installer.
        print('   - Downloading ' + file_name + '... ', end="", flush=True)
        
        # Need to pass in a cookie indicating acceptance of the Oracle license.
        # Reference: https://gist.github.com/scottvrosenthal/11187116
        request = urllib.request.Request(
            jdk_url, 
            None, 
            { 'Cookie': 'oraclelicense=accept-securebackup-cookie' }
        )
        
        # Need to disable SSL verification, due to CMS network buggery.
        # Reference: http://stackoverflow.com/questions/19268548/python-ignore-certicate-validation-urllib2
        ssl_context = ssl.create_default_context()
        ssl_context.check_hostname = False
        ssl_context.verify_mode = ssl.CERT_NONE
        
        # Download and save the file.
        with urllib.request.urlopen(request, context=ssl_context) as response, \
                open(file_path_local, 'wb') as jdk_archive:
            shutil.copyfileobj(response, jdk_archive)
        print('downloaded.')
    else:
        print('   - Installer ' + file_name + ' already downloaded.')
    
    return file_path_local

def jdk_install(jdk_archive_path):
    """
    Extract the specified JDK archive/installer into the `get_tools_dir()`
    directory.
    
    Args:
        jdk_archive_path (str): The local path to the JDK archive/installer to use.
    
    Returns:
        The path to the installed JDK.
    """
    
    # The path to install to.
    _, jdk_name = os.path.split(jdk_archive_path)
    jdk_name = re.sub('\..+$', '', jdk_name)
    jdk_install_path = os.path.join(get_tools_dir(), jdk_name)

    if not os.path.exists(jdk_install_path):
        # Extract the JDK from the archive.
        print('   - Extracting ' + jdk_name + '... ', end="", flush=True)
        if sys.platform == 'cygwin':
           jdk_extract_exe(jdk_archive_path, jdk_install_path)
        else:
            raise OSError('Unsupported platform: ' + sys.platform)
        print('extracted.')
    else:
        print('   - Archive ' + jdk_name + ' already extracted.')
    
    return jdk_install_path

def jdk_extract_exe(jdk_archive_path, jdk_install_path):
    """
    Extract the specified Oracle JDK EXE installer into the `get_tools_dir()`
    directory.
    
    Reference: <http://superuser.com/a/896186>.
    
    Args:
        jdk_archive_path (str): The local path to the Oracle JDK EXE installer to 
            use.
        jdk_install_path (str): The path to install the JDK to.
    
    Returns:
        (nothing)
    """
    
    # Run Cygwin's cabextract to pull out the contents from the EXE.
    cabextract_cmd = [
        'cabextract', 
        # Will match `tools.zip` and `src.zip`.
        '-F', '*.zip',
        '-d', jdk_install_path, 
        jdk_archive_path
    ]
    subprocess.check_call(cabextract_cmd,  
            stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    
    # Extract `tools.zip` right into the install directory, then remove it.
    # It contains the JDK and JRE both, in the correct install layout.
    tools_zip = os.path.join(jdk_install_path, 'tools.zip')
    extract_archive(tools_zip, jdk_install_path)
    os.remove(tools_zip)

    # Mark unpack200.exe as executable.
    unpack_exe = os.path.join(jdk_install_path, 'jre', 'bin', 'unpack200.exe')
    os.chmod(unpack_exe, stat.S_IRWXU | stat.S_IRWXG | stat.S_IRWXO)
    
    # Unpack all of the JAR `*.pack` files that were in `tools.zip`.
    for path, dirs, files in os.walk(jdk_install_path):
        for pack_file in fnmatch.filter(files, '*.pack'):
            jar_file, _ = os.path.splitext(pack_file)
            jar_file = jar_file + '.jar'
            unpack_cmd = [
                unpack_exe,
                '--remove-pack-file',
                cygpath_to_windows(os.path.join(path, pack_file)),
                cygpath_to_windows(os.path.join(path, jar_file))
            ]
            # Note: If this starts throwing odd errors, try replacing the
            # permissions on the workspaces directory and its descendants.
            subprocess.check_call(unpack_cmd,  
                    stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)

def jdk_config_env(jdk_install_path):
    """
    Configures the environment variables, etc. for the specified JDK.
    
    Args:
        jdk_install_path (str): The local path for the JDK install.
    
    Returns:
        (nothing)
    """
    
    # Set JDK_HOME and add the JDK to the path.
    print('   - Updating .bashrc... ', end="", flush=True)
    bash_var_export('JAVA_HOME', jdk_install_path)
    bash_path_include('${JAVA_HOME}/bin')
    print('done.')

def eclipse_download():
    """
    Download the Eclipse archive/installer from eclipse.org.

    Returns:
        The path to the downloaded file, which will be saved into the
        `get_installers_dir()` directory.
    """
    
    # The URL to download from.
    # Note: A little tricky to find this link, as Eclipse now _wants_ you to
    # just download an installer, rather than the full packge. Found this link
    # here: <https://www.eclipse.org/downloads/eclipse-packages/>.
    eclipse_urls = {
        'cygwin': 'http://download.eclipse.org/technology/epp/downloads/release/oxygen/3a/eclipse-jee-oxygen-3a-win32-x86_64.zip'
    }
    eclipse_url = eclipse_urls[sys.platform]
    
    # The path to save the installer to.
    file_name = urlsplit(eclipse_url).path.split('/')[-1]
    file_path_local = os.path.join(get_installers_dir(), file_name)
    
    print('* Install Eclipse')
    
    if not os.path.exists(file_path_local):
        # Download the installer.
        print('   - Downloading ' + file_name + '... ', end="", flush=True)
        with urllib.request.urlopen(eclipse_url) as response, \
                open(file_path_local, 'wb') as eclipse_archive:
            shutil.copyfileobj(response, eclipse_archive)
        print('downloaded.')
    else:
        print('   - Installer ' + file_name + ' already downloaded.')
    
    return file_path_local

def eclipse_install(eclipse_archive_path, jdk_install_path):
    """
    Extract the specified Eclipse archive/installer into the `get_tools_dir()`
    directory.
    
    Args:
        eclipse_archive_path (str): The local path to the archive/installer to
            extract Eclipse from.
        jdk_install_path (str): The local path to the JDK that Eclipse should 
            use.
    
    Returns:
        The path that Eclipse was installed to.
    """
    
    # The path to install to.
    _, eclipse_name = os.path.split(eclipse_archive_path)
    eclipse_name = re.sub('\..+$', '', eclipse_name)
    eclipse_install_path = os.path.join(get_tools_dir(), eclipse_name)
    eclipse_install_path_tmp = os.path.join(get_tools_dir(), eclipse_name + "-tmp")

    if not os.path.exists(eclipse_install_path):
        # Extract the Eclipse install.
        print('   - Extracting ' + eclipse_name + '... ', end="", flush=True)
        extract_archive(eclipse_archive_path, eclipse_install_path_tmp)

        # Make the extracted 'eclipse...-tmp/eclipse' directory the actual 
        # install.
        shutil.move(os.path.join(eclipse_install_path_tmp, 'eclipse'), 
                eclipse_install_path)
        os.rmdir(eclipse_install_path_tmp)
        print('extracted.')
    else:
        print('   - Archive ' + eclipse_name + ' already extracted.')
        
    # Edit the `eclipse.ini` file.
    # Note: Even on Windows, this file only uses '\n's.
    print('   - Updating eclipse.ini... ', end="", flush=True)
    eclipse_ini = os.path.join(eclipse_install_path, 'eclipse.ini')
    jvm_exe = cygpath_if_windows(os.path.join(jdk_install_path, 'bin', 'javaw'))
    with open(eclipse_ini, 'r', encoding='ascii') as eclipse_ini_handle:
        eclipse_ini_contents = eclipse_ini_handle.read()
    eclipse_ini_contents = re.sub(
        '-vm\n.+\n',
        '',
        eclipse_ini_contents,
        re.M
    )
    eclipse_ini_contents = eclipse_ini_contents.replace(
        '-vmargs\n',
        '-vm\n' + jvm_exe + '\n-vmargs\n'
    )
    with open(eclipse_ini, 'w', encoding='ascii') as eclipse_ini_handle:
        eclipse_ini_handle.write(eclipse_ini_contents)
    print('updated.')
        
    return eclipse_install_path

def eclipse_create_shortcut(eclipse_install_path):
    """
    Creates an OS launcher shortcut to the specified Eclipse installation. 
    
    Will replace any existing shortcut for that installation.
    
    Args:
        eclipse_install_path (str): The local path for the Eclipse install.
    
    Returns:
        (nothing)
    """

    if sys.platform == 'cygwin':
        eclipse_create_shortcut_cygwin(eclipse_install_path)
    else:
        raise OSError('Unsupported platform: ' + sys.platform)

def eclipse_create_shortcut_cygwin(eclipse_install_path):
    """
    Creates a Windows Start shortcut to the specified Eclipse installation. 
    
    Will replace any existing shortcut for that installation.
    
    Args:
        eclipse_install_path (str): The local path for the Eclipse install.
    
    Returns:
        (nothing)
    """
    
    # The launcher/shortcut file path.
    _, eclipse_name = os.path.split(eclipse_install_path)
    create_shortcut_cmd = [
        'mkshortcut',
        '--desc=Eclipse IDE',
        '--smprograms',
        '--name=' + eclipse_name,
        os.path.join(eclipse_install_path, 'eclipse')
    ]
    subprocess.check_call(create_shortcut_cmd,  
            stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    print('   - Launcher created/updated.')

def eclipse_install_plugins(eclipse_install_path):
    """
    Installs/updates the plugins in the specified Eclipse installation.
    
    Args:
        eclipse_install_path (str): The local path for the Eclipse install.
    
    Returns:
        (nothing)
    """
    
    # The plugins to install. Each PluginGroup is a collection of IUs to 
    # install together. Each Plugin is an IU with a specific version. Locking 
    # the versions is important, as otherwise there's no guarantee that this
    # script's results will be stable in the future.
    Plugin = collections.namedtuple('Plugin', ['iu', 'version'])
    PluginGroup = collections.namedtuple('PluginGroup', ['name', 'plugins', 'repos'])
    plugin_groups = []
    
    # Enables easy APT code generation for Maven projects.
    apt_plugins = PluginGroup('Maven Integration for Eclipse JDT APT', 
            [Plugin('org.jboss.tools.maven.apt.feature.feature.group', '1.5.0.201805160042')],
            ['http://download.jboss.org/jbosstools/updates/m2e-extensions/m2e-apt'])
    plugin_groups.append(apt_plugins)
    
    # The Eclipse executable.
    eclipse_exe = os.path.join(eclipse_install_path, 'eclipse')
    
    # Install the plugins, one at a time. This is slower, but makes debugging 
    # problems a lot simpler.
    print('* Install Eclipse Plugins')
    for plugin_group in plugin_groups:
        print('   - Installing ' + plugin_group.name + '... ', end="", flush=True)
        
        # Build the comma-separated list of repos for this install.
        repos = ','.join(plugin_group.repos)
        
        # Build the list of '-installIU' args for this install.
        ius_install_args = []
        for plugin in plugin_group.plugins:
            ius_install_args.extend(['-installIU', '{}/{}'.format(plugin.iu, plugin.version)])
        
        # Build the full list of args for Eclipse.
        eclipse_cmd = [eclipse_exe, 
                '-nosplash', 
                '-application', 'org.eclipse.equinox.p2.director',
                '-destination', cygpath_if_windows(eclipse_install_path),
                '-repository', repos]
        eclipse_cmd.extend(ius_install_args)
        
        # Run Eclipse (headless) to install the plugins.
        subprocess.check_call(eclipse_cmd, 
                stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
        
        print('done.')

def maven_download():
    """
    Download the Maven archive/installer from maven.apache.org.

    Returns:
        The path to the downloaded file, which will be saved into the
        `get_installers_dir()` directory.
    """
    
    # The URL to download from.
    maven_urls = {
        'cygwin': 'http://www.us.apache.org/dist/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.zip'
    }
    maven_url = maven_urls[sys.platform]
    
    # The path to save the installer to.
    file_name = urlsplit(maven_url).path.split('/')[-1]
    file_path_local = os.path.join(get_installers_dir(), file_name)
    
    print('* Install Maven')
    
    if not os.path.exists(file_path_local):
        # Download the installer.
        print('   - Downloading ' + file_name + '... ', end="", flush=True)
        with urllib.request.urlopen(maven_url) as response, \
                open(file_path_local, 'wb') as maven_archive:
            shutil.copyfileobj(response, maven_archive)
        print('downloaded.')
    else:
        print('   - Installer ' + file_name + ' already downloaded.')
    
    return file_path_local

def maven_install(maven_archive_path):
    """
    Extract the specified Maven archive/installer into the `get_tools_dir()`
    directory.
    
    Args:
        maven_archive_path (str): The local path to the archive/installer to
            extract Maven from.
    
    Returns:
        The path to the downloaded file, which will be saved into the
        `get_installers_dir()` directory.
    """
    
    # The path to install to.
    _, maven_name = os.path.split(maven_archive_path)
    maven_name = re.sub('-bin\.zip$', '', maven_name)
    maven_install_path = os.path.join(get_tools_dir(), maven_name)
    maven_install_path_tmp = os.path.join(get_tools_dir(), maven_name + "-tmp")

    if not os.path.exists(maven_install_path):
        # Extract the Maven install.
        print('   - Extracting ' + maven_name + '... ', end="", flush=True)
        extract_archive(maven_archive_path, maven_install_path_tmp)

        # Make the extracted 'apache-maven...-tmp/apache-maven-3.x.x' directory the actual 
        # install.
        shutil.move(os.path.join(maven_install_path_tmp, maven_name), 
                maven_install_path)
        os.rmdir(maven_install_path_tmp)
        print('extracted.')
    else:
        print('   - Archive ' + maven_name + ' already extracted.')
    
    return maven_install_path

def maven_config_env(maven_install_path, jdk_install_path):
    """
    Configures the environment variables, etc. for the specified Maven installation.
    
    Args:
        maven_install_path (str): The local path for the Maven install.
        jdk_install_path (str): The local path for the JDK install.
    
    Returns:
        (nothing)
    """
    
    # Set MAVEN_HOME and add Maven to the path.
    print('   - Updating .bashrc... ', end="", flush=True)
    bash_var_export('MAVEN_HOME', maven_install_path)
    bash_path_include('${MAVEN_HOME}/bin')
    print('done.')

    # Create the settings.xml file.
    print('   - Creating settings.xml... ', end="", flush=True)
    settings_path = os.path.join(get_maven_user_dir(), 'settings.xml')
    if not os.path.isfile(settings_path):
        # Copy the distribution file as a starting point.
        settings_source_path = os.path.join(maven_install_path, 'conf', 'settings.xml')
        shutil.copyfile(settings_source_path, settings_path)

        # Then add the required profile to that (if on Cygwin).
        if sys.platform == 'cygwin':
            profile_windows_xml =  etree.fromstring((
                    "    <profile>\n"
                    "      <id>windows-config</id>\n"
                    "      <activation>\n"
                    "        <os>\n"
                    "          <family>windows</family>\n"
                    "        </os>\n"
                    "      </activation>\n"
                    "      <properties>\n"
                    "        <bash.exe>{}</bash.exe>\n"
                    "      </properties>\n"
                    "    </profile>\n"
                    ).format(cygpath_to_windows('/usr/bin/bash')))
            settings_xml = etree.parse(settings_path)
            settings_xml.find('{http://maven.apache.org/SETTINGS/1.0.0}profiles').append(profile_windows_xml)
            settings_xml.write(settings_path)
        print('done.')
    else:
        print('already present.' + settings_path)

    # Create the toolchains.xml file.
    print('   - Updating toolchains.xml... ', end="", flush=True)
    toolchains_path = os.path.join(get_maven_user_dir(), "toolchains.xml")
    if not os.path.isfile(toolchains_path):
        toolchains_xml = (
                "<?xml version=\"1.0\" encoding=\"UTF8\"?>\n"
                "<toolchains>\n"
                "  <toolchain>\n"
                "    <type>jdk</type>\n"
                "    <provides>\n"
                "      <version>1.8</version>\n"
                "      <vendor>oracle</vendor>\n"
                "    </provides>\n"
                "    <configuration>\n"
                "      <jdkHome>{}</jdkHome>\n"
                "    </configuration>\n"
                "  </toolchain>\n"
                "</toolchains>\n"
                ).format(cygpath_to_windows(jdk_install_path))
        with open(toolchains_path, "w+") as toolchains_file:
            toolchains_file.writelines(toolchains_xml)
        print('done.')
    else:
        print('already present.')

def get_workspaces_dir():
    """
    Return the path to the directory to store development files in.
    
    Create the directory if it does not already exist.
    
    Returns:
        The path to the directory to store development files in.
    """
    
    workspaces_dir = os.path.join(os.path.expanduser('~'), 'workspaces')
    if not os.path.isdir(workspaces_dir):
        if sys.platform == 'cygwin':
            # First, create the `c:\workspaces\` directory if it doesn't exist.
            root_workspaces_dir = os.path.join('/cygdrive', 'c', 'workspaces')
            os.makedirs(root_workspaces_dir, exist_ok=True)
            
            # Then, create a symbolic link to it at `~/workspaces`.
            os.symlink(root_workspaces_dir, workspaces_dir)
        else:
            raise OSError('Unsupported platform: ' + sys.platform)
        
    return workspaces_dir

def get_tools_dir():
    """
    Return the path to the directory to install development tools to.
    
    Create the directory if it does not already exist.
    
    Returns:
        The path to the directory to install development tools to.
    """
    
    tools_dir = os.path.join(get_workspaces_dir(), 'tools')
    os.makedirs(tools_dir, exist_ok=True)
    return tools_dir

def get_installers_dir():
    """
    Return the path to the directory to save installers to.
    
    Create the directory if it does not already exist.
    
    Returns:
        The path to the directory to save installers to.
    """
    
    installers_dir = os.path.join(get_tools_dir(), 'installers')
    os.makedirs(installers_dir, exist_ok=True)
    return installers_dir

def get_maven_user_dir():
    """
    Return the path to the user's .m2 directory.
    
    Create the directory if it does not already exist.
    
    Returns:
        The path to the user's .m2 directory.
    """
    
    if sys.platform == 'cygwin':
        user_dir = cygpath_to_unix(os.environ['USERPROFILE'])
    else:
        raise OSError('Unsupported platform: ' + sys.platform)
    maven_user_dir = os.path.join(user_dir, '.m2')
    os.makedirs(maven_user_dir, exist_ok=True)
    return maven_user_dir

def extract_archive(archive, destination):
    """
    Extracts the specified `.zip` or `.tar.gz` archive to the specified 
    destination. Please note that this function provides no special handling
    for whether or not the specified archive has a single top-level directory
    itself or not: the contents of the archive will all be placed in the 
    specified directory, overwriting any pre-existing files.
    
    Args:
        archive (str): The local path to the archive to be extracted.
        destination (str): The local path of the directory to extract into.
    
    Returns:
        (nothing)
    """
    
    if archive.endswith('.zip'):
        with zipfile.ZipFile(archive, "r") as zip_handle:
            zip_handle.extractall(destination)
    elif archive.endswith('.tar.gz'):
        with tarfile.open(archive) as tar_handle:
            tar_handle.extractall(destination)
    else:
        raise Exception('Unsupported archive type: ' + archive)

def cygpath_to_windows(path):
    """
    Uses Cygwin's `cygpath` utility to convert a path to Windows format, e.g.
    `C:\FOO\bar.txt`.
    
    Args:
        path (str): The path to be converted.
    
    Returns:
        A new path, in Windows format.
    """
    
    cygpath_cmd = [
        'cygpath', 
        '--windows', 
        '--absolute',
        '--codepage', 'UTF8',
        path
    ]
    windows_path = subprocess.check_output(cygpath_cmd,
        stderr=subprocess.STDOUT)
    return windows_path.decode('utf8').rstrip('\n')

def cygpath_to_unix(path):
    """
    Uses Cygwin's `cygpath` utility to convert a path to Windows format, e.g.
    `/cygdrive/c/foo/bar`.
    
    Args:
        path (str): The path to be converted.
    
    Returns:
        A new path, in Unix/Cygwin format.
    """
    
    cygpath_cmd = [
        'cygpath',
        '--unix',
        '--absolute',
        '--codepage', 'UTF8',
        path
    ]
    unix_path = subprocess.check_output(cygpath_cmd,
        stderr=subprocess.STDOUT)
    return unix_path.decode('utf8').rstrip('\n')

def cygpath_if_windows(path):
    """
    Returns the result of `cygpath_to_windows` for the specified path if this
    script is being run inside Cygwin. Otherwise, returns the path as-is.
    
    Args:
        path (str): The path to be converted.
    
    Returns:
        A new path, in Windows format (maybe).
    """
    
    if sys.platform == 'cygwin':
        return cygpath_to_windows(path)
    else:
        return path

def bash_var_export(name, value):
    """
    Sets the specified environment variable in the user's `.bashrc` file.
    
    This isn't the world's most intelligent function: it searches `.bashrc`
    for a line that begins with "`export varname=`". If such a line is found,
    it's replaced. Otherwise, such a line is added to the end of the file.
    
    Args:
        name (str): The name of the environment variable to set.
        value (str): The value to set the environment variable to.
    
    Returns:
        (nothing)
    """
    
    # The .bashrc path to read from (and eventually replace).
    bashrc_path = os.path.join(os.path.expanduser('~'), '.bashrc')
    
    # The .bashrctmp path to write to.
    bashrctmp_path = ''
    with tempfile.NamedTemporaryFile(delete=False) as bashrc_tmp:
        bashrctmp_path = bashrc_tmp.name
    
    # Open two files:
    #  1. .bashrc, for reading
    #  2. A temp file to write the modified contents of .bashrc out to.
    var_found = False
    with open(bashrc_path, 'r') as bashrc, \
         open(bashrctmp_path, 'w') as bashrc_tmp:
        for line in bashrc:
            if line.startswith('export {}='.format(name)):
                var_found = True
                bashrc_tmp.write('export {}={}\n'.format(name, value))
            else:
                bashrc_tmp.write(line)
        if not var_found:
            bashrc_tmp.write('export {}={}\n'.format(name, value))
    
    # Replace the original .bashrc with the modified one.
    bashrc_stat = os.stat(bashrc_path)
    os.chmod(bashrctmp_path, stat.S_IMODE(bashrc_stat.st_mode))
    os.chown(bashrctmp_path, bashrc_stat.st_uid, bashrc_stat.st_gid)
    os.replace(bashrctmp_path, bashrc_path)

def bash_path_include(directory):
    """
    Adds the specified directory to the path in the user's `.bashrc` file.
    
    This isn't the world's most intelligent function: it searches `.bashrc`
    for an `export PATH=...` line matching the one being added. If such a line
    is found, it's replaced. Otherwise, such a line is added to the end of the 
    file.
    
    Args:
        directory (str): The directory to add to the path.
    
    Returns:
        (nothing)
    """
    
    # The .bashrc path to read from (and eventually replace).
    bashrc_path = os.path.join(os.path.expanduser('~'), '.bashrc')
    
    # The .bashrctmp path to write to.
    bashrctmp_path = ''
    with tempfile.NamedTemporaryFile(delete=False) as bashrc_tmp:
        bashrctmp_path = bashrc_tmp.name
    
    # The line being added to the file.
    path_entry = 'export PATH=${PATH}:' + directory + '\n'
    
    # Open two files:
    #  1. .bashrc, for reading
    #  2. A temp file to write the modified contents of .bashrc out to.
    path_entry_found = False
    with open(bashrc_path, 'r') as bashrc, \
            open(bashrctmp_path, 'w') as bashrc_tmp:
        for line in bashrc:
            if line == path_entry:
                path_entry_found = True
            bashrc_tmp.write(line)
        if not path_entry_found:
            bashrc_tmp.write(path_entry)
    
    # Replace the original .bashrc with the modified one.
    bashrc_stat = os.stat(bashrc_path)
    os.chmod(bashrctmp_path, stat.S_IMODE(bashrc_stat.st_mode))
    os.chown(bashrctmp_path, bashrc_stat.st_uid, bashrc_stat.st_gid)
    os.replace(bashrctmp_path, bashrc_path)

# If this file is being run as a standalone script, call the main() function.
# (Otherwise, do nothing.)
if __name__ == "__main__":
    main()

