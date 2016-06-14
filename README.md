# CommandLine
Set of classes and annotations for creating Java command line applications

This project lets you write a Java command line application fairly simply. The project includes annotations that
allow you to mark routines to be called when an option is found on the command line, as well as a method to call
when as the main application with the remaining command line arguments.

For example:



public class Main extends CommandLineApplication {

    // Mark this routine to be called when the -v or --version
    // switches are given on the command line. Since these methods
    // return boolean, if they return false the main method will 
    // not be called after the options are processed.
    @CommandLineOption( shortForm = "v", longForm = "version", usage = "Show the version number and quit" )
    public boolean showVersion() {
        System.out.println( "Main command line app V1.0" );
        return false;
    }

    // This routine is similar to the above, except it leverages
    // a method printCommandLineUsageText() which provides a usage
    // text based on the configured options. This usage message
    // is dependent on the "usage" argument of the CommandLineOption
    // parameters. As before, this routine returns false to prevent the 
    // main app from executing
    @CommandLineOption( shortForm = "h", longForm = "help", usage = "Show this message and exit" )
    public boolean showHelp() {
        printCommandLineUsageText("Jutzo v0.1", "", "" );
        return false;
    }

    // This method will be called with an argument when -s or --switch is 
    // provided - e.g. --switch=someValue. Since the method doesn't want
    // to stop processing, rather than returning TRUE it can just declare
    // itself as having no return
    @CommandLineOption( shortForm = "s", longForm = "switch", usage = "switch with arguments", argumentType = String.class )
    public void scanSwitch(String packageToScan) {
        // Do something with the argument
    }

    // The main program logic goes here, marked by @CommandLineMain.
    // This method will be called with any non-option command line
    // arguments, so long as no option method returns FALSE. Note 
    // that this ONLY gets the remaining arguments; the ones that
    // contain options will not be passed.
    @CommandLineMain
    public void run(String args[]) {
        System.out.println( "Hello world!" );
    }

    // This all works by calling parseAndRun on the 
    // class. This will scan for the options, set up the
    // common-cli options elements, parse the command line,
    // handle the options, and then invoke the @CommandLineMain
    // routine 
    public static void main(String[] args) {
        try {
            Main main = new Main();
            main.parseAndRun(args);
        } catch (CommandLineException e) {
            System.out.println( "Unable to run application because " + e.getMessage());
            e.printStackTrace();
        }
    }
}
