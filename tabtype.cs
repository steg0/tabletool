using System;
using System.Diagnostics;
using System.IO;

namespace Tabtype
{
    /* 
     * A launcher for Windows. For this to work, copy the JAR as well as the
     * dependency/ directory to the same location as this executable.
     * Optional configuration files (the most useful is javaw.conf which 
     * contains the path to javaw.exe) need to be placed in %APPDATA%\tabtype.
     */
    public class Tabtype
    {
        static void Main(string[] args)
        {
            string procbin = "javaw.exe";
            string javaConfigPath = Environment.ExpandEnvironmentVariables(
                    "%APPDATA%\\tabtype\\javaw.conf");
            if(File.Exists(javaConfigPath))
                using(StreamReader sr = new StreamReader(javaConfigPath))
                {
                    procbin = sr.ReadLine();
                }
            string appDirectory = AppDomain.CurrentDomain.BaseDirectory;
            string[] deps = Directory.GetFiles(appDirectory + "dependency");
            string procargs=Environment.ExpandEnvironmentVariables(
                    "-XX:+UseSerialGC " +
                    "-Dfile.encoding=UTF-8 " +
                    "-Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel " +
                    "-p " +
                    "\"" + String.Join(";",deps).Replace("\"","\"\"") + "\" " +
                    "-jar " + appDirectory.Replace("\"","\"\"") + "tabtype.jar " +
                    "-logconfig %APPDATA%\\tabtype\\logging.properties " +
                    "-config %APPDATA%\\tabtype\\tabtype.properties.xml " +
                    "-config %APPDATA%\\tabtype\\tabtype.properties " +
                    "-- ");

            foreach(string arg in args)
            {
                procargs += " \"" + arg.Replace("\"","\"\"") + "\"";
            }

            ProcessStartInfo pStartInfo = new ProcessStartInfo(procbin,
                    procargs);
            pStartInfo.UseShellExecute = false;
            pStartInfo.CreateNoWindow = true;

            Process proc = new Process();
            proc.StartInfo = pStartInfo;

            proc.Start();
        }
    }
}
