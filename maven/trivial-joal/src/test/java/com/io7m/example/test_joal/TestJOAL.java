package com.io7m.example.test_joal;

import org.junit.Test;

import com.jogamp.openal.AL;
import com.jogamp.openal.ALException;
import com.jogamp.openal.ALFactory;

public class TestJOAL
{
  @SuppressWarnings("static-method") @Test public void go()
  {
    try {
      final AL al = ALFactory.getAL();
      al.alGetError();
      System.err.println("AL: " + al);
    } catch (final ALException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
