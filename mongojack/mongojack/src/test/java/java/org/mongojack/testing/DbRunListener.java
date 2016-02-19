package org.mongojack.testing;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

/**
 * @author Ben McCann (benmccann.com)
 */
public class DbRunListener extends RunListener {

  
  public void testRunStarted(Description description) throws Exception {
    DbManager.startDb();
  }

  
  public void testRunFinished(Result result) throws Exception {
    DbManager.stopDb();
  }

}
