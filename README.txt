This is a telnet client, intended for use with telnet-based talkers.

Its purpose is not to be better than existing telnet clients, but to
provide a workaround for talkers which do not recognize telnet's BINARY
option, and thus are restricted to ASCII:  This client sends non-ASCII
characters as {U+nnnnnn}.  Conversely, when it sees that pattern in a
socket's output, it displays the pattern as the corresponding Unicode
codepoint.  For example, if {U+221e} is received, you will see an infinity
symbol.

To build, you need JDK 9 or later, and you need Ant,
from <http://ant.apache.org>.  Version 1.10.1 or later
is recommended.  Older versions of Ant may or may not work.

Note that Ant usually requires the JAVA_HOME environment variable
to be set to the directory where the JDK resides.

The only commands you are likely to care about are:

- ant executable
- ant tar
- ant zip

As with any Ant-based project, you can run "ant -p" while in this directory
to list all supported build targets.
