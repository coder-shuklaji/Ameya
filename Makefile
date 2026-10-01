# ==== Ameya Interpreter Makefile ====
#
# Layout expected:
#   com/interpreter/ameya/Ameya.java
#   com/interpreter/ameya/lexer/*.java
#   com/interpreter/ameya/parser/*.java
#
# Output:
#   com/interpreter/ameya/bin/   <- Ameya.java + lexer/ + parser/ classes
#   (javac follows the dependency chain automatically, so compiling
#    Ameya.java alone pulls in every class it references.)
#
# Usage:
#   make                          -> compiles Ameya.java (and everything it needs)
#   make run                      -> compiles, then runs the REPL prompt
#   make run FILE=sample.am       -> compiles, then runs that script
#                                     (NOTE: no spaces around "=", exact case "FILE")
#   make compile                  -> compile only, no run
#   make clean                    -> removes bin/

JAVAC := javac
JAVA  := java

PKG_ROOT   := com/interpreter/ameya
BIN_DIR    := $(PKG_ROOT)/bin
MAIN_CLASS := com.interpreter.ameya.Ameya
MAIN_FILE  := $(PKG_ROOT)/Ameya.java

.PHONY: all compile run clean

all: compile

# Always recompiles -- no staleness cache, since the interpreter is actively
# being written and a fresh compile is fast anyway.
# Passing only Ameya.java to javac is enough: javac automatically finds and
# compiles every class it depends on (lexer/, parser/, etc.) alongside it.
compile:
	@mkdir -p $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(MAIN_FILE)

run: compile
	$(JAVA) -cp $(BIN_DIR) $(MAIN_CLASS) $(FILE)

clean:
	rm -rf $(BIN_DIR)