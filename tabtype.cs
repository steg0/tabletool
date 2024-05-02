using System;
using System.Diagnostics;

namespace Tabtype
{
    public class Tabtype
    {
        static void Main(string[] args)
        {
            string procbin=Environment.ExpandEnvironmentVariables(
                "%USERPROFILE%\\proj\\jdk-21.0.1\\bin\\javaw.exe");
            string procargs=Environment.ExpandEnvironmentVariables("-XX:+UseSerialGC " +
                "-Dfile.encoding=UTF-8 " +
                "-Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel " +
                "-Djava.util.logging.config.file=%USERPROFILE%\\proj\\tabtype\\runtime.logging.properties " +
                "-p " +
                "%USERPROFILE%\\.m2\\repository\\org\\apache\\derby\\derbyshared\\10.15.2.0\\derbyshared-10.15.2.0.jar;" +
                "%USERPROFILE%\\.m2\\repository\\org\\apache\\derby\\derby\\10.15.2.0\\derby-10.15.2.0.jar;" +
                "%USERPROFILE%\\.m2\\repository\\org\\postgresql\\postgresql\\42.3.1\\postgresql-42.3.1.jar;" +
                "%USERPROFILE%\\.m2\\repository\\com\\oracle\\database\\jdbc\\ojdbc11\\21.7.0.0\\ojdbc11-21.7.0.0.jar;" +
                "%USERPROFILE%\\.m2\\repository\\com\\oracle\\database\\xml\\xdb\\21.7.0.0\\xdb-21.7.0.0.jar;" +
                "%USERPROFILE%S\\.m2\\repository\\com\\ibm\\db2\\jcc\\11.5.7.0\\jcc-11.5.7.0.jar " +
                "-jar %USERPROFILE%\\proj\\tabtype\\tabtype.jar " +
                "-config %APPDATA%\\tabtype\\tabtype.properties " +
                "-config %USERPROFILE%\\Documents\\src\\tabtype\\test.tabtype.xml " +
                "-- ");

            foreach(string arg in args)
            {
                procargs += " \"" + arg.Replace("\"","\"\"") + "\"";
            }

            ProcessStartInfo pStartInfo = new ProcessStartInfo(
                procbin,procargs);
            pStartInfo.UseShellExecute = false;
            pStartInfo.CreateNoWindow = true;

            Process proc = new Process();
            proc.StartInfo = pStartInfo;

            proc.Start();
        }
    }
}
