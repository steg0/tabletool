@set MODULEPATH=%USERPROFILE%\.m2\repository\com\oracle\ojdbc10\19.3\ojdbc10-19.3.jar;%USERPROFILE%\.m2\repository\com\ibm\db2\jcc\11.1.4.4\jcc-11.1.4.4.jar;p:\jdbc_sqlj\db2jcc4.jar
@start %JAVA_HOME%\bin\javaw %JAVA_OPTS% -XX:+UseSerialGC ^
    -Dfile.encoding=UTF-8 ^
    -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel ^
    -jar target/myriad.jar ^
    -config test.myriad.properties ^
    -- ^
    %1 %2 %3 %4 %5 %6 %7 %8 %9
