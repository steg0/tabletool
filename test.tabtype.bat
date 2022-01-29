%JAVA_HOME%\bin\java %JAVA_OPTS% -ea -XX:+UseSerialGC ^
    -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9001 ^
    -Dfile.encoding=UTF-8 ^
    -Djava.util.logging.config.file=test.logging.properties ^
    -p %USERPROFILE%\.m2\repository\org\apache\derby\derbyshared\10.15.2.0\derbyshared-10.15.2.0.jar;%USERPROFILE%\.m2\repository\org\apache\derby\derby\10.15.2.0\derby-10.15.2.0.jar;%USERPROFILE%\.m2\repository\com\oracle\ojdbc10\19.3\ojdbc10-19.3.jar;%USERPROFILE%\.m2\repository\com\ibm\db2\jcc\11.5.7.0\jcc-11.5.7.0.jar;target\classes ^
    -m de.steg0.deskapps.tabletool/de.steg0.deskapps.tabletool.Tabtype ^
    -config test.tabtype.properties ^
    test.tt.xml
