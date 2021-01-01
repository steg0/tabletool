▶ Invocation

The tool builds as modular JAR and can be started up without arguments. However, to be of any practical use, it is necessary to put JDBC drivers on the module path, and use a properties file where connection definitions will be read from.

Also, an optional single file name argument is supported which is understood as "workspace file" (an XML format) where the current set of open SQL files will be persisted to. The file does not need to exist initially.

An example command line is:

┌─────────────────────────────────────────────────────────────────────┐
  java -XX:+UseSerialGC \
    -Dfile.encoding=Cp1252 \
    -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel \
    -p $HOME/.m2/repository/com/oracle/ojdbc7/12.1.0.1/ojdbc7-12.1.0.1.jar \
    -jar tabtype.jar \
    -config $HOME/Documents/tabtype.properties \
    $HOME/Documents/workspace.tt.xml
└─────────────────────────────────────────────────────────────────────┘

This example uses the serial GC, which is a recommendation for desktop applications, sets a default encoding for text files, and uses the Windows L&F.


▶ Property file format

The configuration file supports the following keys:

┌─────────────────────────────────────────────────────────────────────┐
  frame.x=100
  frame.y=100
  frame.w=700
  frame.h=450
  default.bg=#ffffff
  scroll.increment=16
  resultview.height=150
  connections.<Name 1>.url=<JDBC URL>
  connections.<Name 1>.username=<User>
  connections.<Name 1>.password=<Password>
  connections.<Name 1>.bg=#eeeedd
└─────────────────────────────────────────────────────────────────────┘

More than one connection definition can occur in the file as long as the name part (after "connections.") is different.


▶ Actions in a notebook

While editing SQL in a notebook, the following keys are supported:

• Ctrl+Enter - submit the query under cursor or (if present) the selected text. 
• Ctrl+Shift+Enter - like Ctrl+Enter, but always create a new result table.
• Ctrl+Tab - select the next result table.
• Ctrl+Shift+Tab - select the previous result table.
• Ctrl+/ - comment/uncomment.
• Ctrl+Z, Ctrl+Y - undo, redo.
• Ctrl+Up - focus tab title.

To navigate across a result table, use Up/Down arrow keys.

In a result table, a double click on a cell brings up a window to view the cell content in a larger space. For CLOBs, this fetches the complete content instead of displaying the standard Object::toString(). For BLOBs, it displays the first couple of bytes as a dump but offers export/import functionality. For DB2, connect with the "progressiveStreaming=2" option to be able to export BLOBs. Also see notes about the import function below.

A right click on the result table brings up a popup menu which allows closing the table. This also closes any underlying ResultSet. Normally, the tool leaves ResultSets open, but closes them when:

Ⓐ a subsequent query is submitted over the connection; and, as mentioned,
Ⓑ the result table is closed (either with the popup action or by closing the tab).

Some JDBC drivers close ResultSets automatically when the cursor moves
beyond the last row.


▶ Submitting blocks

If a query begins with "{", "create", "call", "begin", or "declare", CallableStatement is used to submit it. Note that there are differences between database products when it comes to what actually can be submitted this way. Generally, Oracle expects a trailing semicolon after the END that closes the block, while DB2 does not.


▶ Update function for BLOBs

The single benefit (right now) of leaving the ResultSet open is that updatable ResultSets can be made available to the user. This is utilized to allow BLOB value updates. There are a few prerequisites for this to work:

Ⓐ don't fetch beyond the last row. If you did, fetch again but set a manual, small enough fetch size in the 〈Fetch:〉 field before.
Ⓑ use explicit columns instead of SELECT * in the query.
Ⓒ use SELECT FOR UPDATE instead of SELECT.

There is some difference between JDBC drivers when it comes to this feature. DB2 drivers will error out if Ⓐ and Ⓑ are not met. Oracle drivers show an error but can still update the value (it might be best not to rely on this particular behavior). DB2 might not need the FOR UPDATE, but Oracle generally will.