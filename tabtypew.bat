@start %JAVA_HOME%\bin\javaw %JAVA_OPTS% -XX:+UseSerialGC ^
    -Dfile.encoding=UTF-8 ^
    -Djava.util.logging.config.file=runtime.logging.properties ^
    -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel ^
    -p %USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.3.1\postgresql-42.3.1.jar;%USERPROFILE%\.m2\repository\com\oracle\ojdbc10\19.3\ojdbc10-19.3.jar;%USERPROFILE%\.m2\repository\com\ibm\db2\jcc\11.1.4.4\jcc-11.1.4.4.jar ^
    -jar target/tabtype.jar ^
    -config test.tabtype.properties ^
    -- ^
    %1 %2 %3 %4 %5 %6 %7 %8 %9
