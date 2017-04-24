package casper;

import java.util.Arrays;

/**
 * Main is the main program of the compiler extension.
 * It simply invokes Polyglot's main, passing in the extension's
 * ExtensionInfo.
 */
public class Main
{
  public static void main(String[] args) {
      polyglot.main.Main polyglotMain = new polyglot.main.Main();

      try {
    	  if(args.length > 5 && args[5].equals("slow")){
    		  Configuration.slow = true;
    		  polyglotMain.start(Arrays.copyOfRange(args, 0, 5), new casper.ExtensionInfo());
    	  }
    	  else{
    		  polyglotMain.start(args, new casper.ExtensionInfo());
    	  }
      }
      catch (polyglot.main.Main.TerminationException e) {
          System.err.println(e.getMessage());
          System.exit(1);
      }
  }
}
