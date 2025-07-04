▶ Invocation

In addition to options dealing with JVM specifics, the tool supports the following command line arguments:

-config <file> - to specify one or more properties files where connection definitions will be read from. For such files, the Java .properties format as well as its XML variant are supported.

-logconfig <file> - to specify a Java Logger properties file. The tool's logger is called "tabtype".

Also, an optional single file name argument, ending with .xml or .tabtype, is supported which is understood as "workspace file" (an XML format) where the current set of open SQL files will be persisted to. The file does not need to exist initially. Other arguments will be understood as SQL files to open.

▶ Property file format

See Help > Show Example Config.

▶ Notebook file format

The tool loads and saves text files, which can contain CSV result sections marked up with a special comment syntax. These are shown as table widgets when opening such a file, to provide a way to carry over results in a pretty way from one session to the next. LOB information will not be part of this format, however.

Focused lines that start with the string "-- connect ", followed by one of the connection names from the property file, enable the menu item Connection > Open, or a submit command, to directly select that connection for the notebook.


▶ Actions in a notebook

While editing SQL in a notebook, in addition to the shortcuts shown in the menu bar, the following keys are supported:

• Alt+Up, Alt+Down - increase/decrease fetch size.
• Alt+Left, Alt+Right - select prior/next tab.
• Ctrl+Alt+Left, Ctrl+Alt+Right - move tab left/right.
• Alt+Enter - open JDBC parameters window.
• Ctrl+Enter - submit the query under cursor or (if present) the selected text. 
• F5, Ctrl+R - like Ctrl+Enter, but reuse the nearest result table.
• Ctrl+Tab - select the next result table or editor pane.
• Ctrl+Shift+Tab - select the previous result table or editor pane.
• Ctrl+1, Ctrl+2, and so on - select tab by index.
• Ctrl+` - select last tab.
• Ctrl+/ - comment/uncomment.
• Ctrl+F - find text (case-insensitively), starting on currently selected tab.
• F3 - find next.
• Ctrl+D - delete line.
• Ctrl+G - go to line.
• Ctrl+Z, Ctrl+Y - undo, redo. These are local to the focused editor section.
• Ctrl+Up - focus tab title.
• F1 - execute infoTemplate for word under cursor or selection.
• F2 - insert snippets into word under cursor or selection.
• F6 - toggle log panel focus.
• F8 - execute completionTemplate for word under cursor or selection.

To navigate across a result table, use Up/Down arrow keys.

In a result table, a double click or Enter on a cell brings up a window to view the cell content in a larger space. For CLOBs, this fetches the complete content instead of displaying the standard Object::toString(). For BLOBs, it displays the first couple of bytes as a dump but offers export/import functionality. For DB2, connect with the "progressiveStreaming=2" option to be able to export BLOBs. Also see notes about the import function below. Ctrl+Backspace closes the table. Note that there is no undo function for any actions on result tables.

A right click on the result table brings up a popup menu which also allows closing the table, or exporting the buffer with desktop actions.

Closing also closes any underlying ResultSet. Normally, the tool leaves ResultSets open, but closes them when:

Ⓐ a subsequent query is submitted over the connection; and, as mentioned,
Ⓑ the result table is closed (either with the UI action or by closing the tab).

Some JDBC drivers close ResultSets automatically when the cursor moves beyond the last row.


▶ Submitting blocks

If a query begins with "{", "call", "begin", or "declare", CallableStatement is used to submit it. Note that there are differences between database products when it comes to what actually can be submitted this way. Generally, Oracle expects a trailing semicolon after the END that closes the block, while DB2 does not.


▶ Update function

The single benefit (right now) of leaving the ResultSet open is that updatable ResultSets can be made available to the user. This is utilized to allow cell value updates. There are a few prerequisites for this to work:

Ⓐ don't fetch beyond the last row. If you did, fetch again but set a manual, small enough fetch size in the 〈Fetch:〉 field before.
Ⓑ use explicit columns instead of SELECT * in the query.
Ⓒ use SELECT FOR UPDATE instead of SELECT.

There is some difference between JDBC drivers when it comes to this feature. DB2 drivers will error out if Ⓐ and Ⓑ are not met. Oracle drivers show an error but can still update the value (it might be best not to rely on this particular behavior). DB2 might not need the FOR UPDATE, but Oracle generally will.
