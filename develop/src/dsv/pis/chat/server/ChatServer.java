// ChatServer.java
// Fredrik Kilander, DSV SU/KTH
// 14-oct-2004/FK New package.
// 25-mar-2004/FK New package.
// 18-mar-2004/FK First version
//
// This program is a simple chat-server using Jini. It answers to requests
// from ChatClient instances, which deposit message strings on the methods
// that implement ChatServerInterface. The message strings are then sent
// back out as CharNotification events to all callbacks that are
// registered with the server.

package dsv.pis.chat.server;

// Standard Java

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.util.LinkedList;
import java.util.Vector;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
//import java.applet.*;
import sun.audio.*;

// Jini

import net.jini.core.entry.*;
import net.jini.core.event.*;
import net.jini.core.lookup.*;
import net.jini.lookup.*;
import net.jini.lookup.entry.*;

/**
 * The ChatServer class is a main program application that implements
 * a simple chat service. It provides service to ChatClient instances
 * which also host the user interface application.
 */
public class ChatServer
  extends
    java.rmi.server.UnicastRemoteObject	// for Java RMI
  implements
    ChatServerInterface,	// for clients
    Runnable			// for the distribution thread.
{
  /**
   * The server's message and image counter. Increments monotonically with each
   * message dispatched.
   */
  protected int msgCount = 0;
  protected int imgCount =0;
  protected int audCount =0;
  /*
  * Bool value to check if an image or audio is received
  */
  
  protected int isImage = 0;
  protected int isAud = 0;

  /**
   * Incoming messages, audio and images are placed on the message queue. The distribution
   * thread consumes the queue by sending copies off to registered clients.
   */
  protected LinkedList msgQueue = new LinkedList ();
  protected LinkedList imgQueue = new LinkedList ();
  protected LinkedList audQueue = new LinkedList ();

  /**
   * The notification objects of registered clients are held in this
   * vector.
   */
  protected Vector clients = new Vector ();

  /**
   * The printed name of this server instance.
   */
  protected String serverName = null;

  /**
   * The join manager is a Jini utility object that helps us being
   * registered with lookup servers.
   */
  protected JoinManager jmgr = null; // part of the jini middleware

  /**
   * The delivery thread runs while this flag is true.
   */
  protected boolean runDelivery = true;

  /**
   * This inner class implements the ServiceIDListener interface on which
   * the Jini join manager will notify us when it has created a service id
   * for us and registered us with the lookup servers.
   */
  private class IDListener implements ServiceIDListener {
    public IDListener () {}
    public void serviceIDNotify (ServiceID sid) {
      System.out.println ("Registered as Jini service " + sid);
    }
  }

  /**
   * Creates a new ChatServer.
   * @param idName The identifying name of this server instance.
   */
  public ChatServer (String idName)
    throws
      java.io.IOException,
      java.rmi.RemoteException,	// if join doesn't work
      java.net.UnknownHostException // if we don't know where we are
  {
    // Find out our hostname so that clients can see it in the registration.

    String host = InetAddress.getLocalHost ().getHostName ().toLowerCase ();
    
    serverName =
      "ChatServer " + ((idName != null) ? idName : "") + " on " + host;

    // Compose the arguments for the registration attempt with the
    // Jini lookup server.

    Entry [] attributes = new Entry [1]; // Entry data type defined by middleware
    attributes[0] = new Name (serverName); // Name : class in middleware

    // Create a Join manager that will hunt out any Jini lookup servers
    // out there and register us with them.

    jmgr = new JoinManager
      (
       this,			// this is the service object
       attributes,		// how we describe ourselves
       new IDListener (),	// to learn of a registration
       null,			// default service discovery manager
       null			// default lease renewal manager
       );

    // Start the service thread.
    new Thread (this).start ();
  }

  /**
   * Shuts down the server by asking the join manager to stop working.
   * This will deregister this ChatServer instance from the lookup
   * servers so they don't deal out dead service objects to clients.
   * The registration usually times out in five minutes but this is,
   * well, <strong>cleaner</strong>.
   */
  protected void shutdown () {
    jmgr.terminate ();
  }

  /**
   * Adds a message the the output queue. This call is synchronized so
   * we have unperturbed access to the modification of the message queue.
   * @param msg  The text message to add.
   */
  protected synchronized void addMessage (String msg) {
    msgQueue.addLast (msg);
    msgCount++;
    System.out.println ("MSG#" + msgCount + ":" + msg);
    // Wake up the distribution thread.
    wakeUp (); // intimates the thread to send the message to wake up.
  }
  
   /**
   * Adds an image to the output queue. This call is synchronized so
   * we have unperturbed access to the modification of the message queue.
   * @param image  The image to add.
   */
  protected synchronized void addImage (ImageIcon image) {
	isImage =1;
    imgQueue.addLast (image);
    imgCount++;
    System.out.println ("IMG#" + imgCount);
    // Wake up the distribution thread.
    wakeUp (); // intimates the thread to send the message to wake up.
  }

   /**
   * Adds an sound to the output queue. This call is synchronized so
   * we have unperturbed access to the modification of the message queue.
   * @param audio  The audio to add.
   */
  protected synchronized void addAud (AudioStream aud) {
	isAud =1;
    audQueue.addLast (aud);
    audCount++;
    System.out.println ("IMG#" + audCount);
    // Wake up the distribution thread.
    wakeUp (); // intimates the thread to send the message to wake up.
  }
  
  /**
   * Retrieves the oldest (first) message from the message queue.
   * This call is synchronized to prevent simultaneous update of
   * the message queue.
   * @return The next message, or null if the queue is empty.
   */
  protected synchronized String getNextMessage () {
    String rtn = null;
    try {
      rtn = (String) msgQueue.removeFirst ();
    }
    catch (java.util.NoSuchElementException nse) {}
    return rtn;
  }
  
   /**
   * Retrieves the oldest (first) audio from the audio queue.
   * This call is synchronized to prevent simultaneous update of
   * the audio queue.
   * @return The next audio, or null if the queue is empty.
   */
  protected synchronized AudioStream getNextAud () {
    AudioStream rtn = null;
    try {
      rtn = (AudioStream) audQueue.removeFirst ();
    }
    catch (java.util.NoSuchElementException nse) {}
    return rtn;
  }
  
  /**
   * Retrieves the oldest (first) image from the image queue.
   * This call is synchronized to prevent simultaneous update of
   * the image queue.
   * @return The next image, or null if the queue is empty.
   */
  protected synchronized ImageIcon getNextImage () {
    ImageIcon rtn = null;
    try {
      rtn = (ImageIcon) imgQueue.removeFirst ();
    }
    catch (java.util.NoSuchElementException nse) {}
    return rtn;
  }

  /**
   * Adds a registration to the list of clients currently connected to
   * this ChatServer instance. This method is synchronized to prevent
   * simultaneous update of the client list.
   * @param rel  The RemoteEventListener implementation to add.
   */
  protected synchronized void addClient (RemoteEventListener rel) {
    clients.add (rel);
    System.out.println ("Added client : " + rel.toString ());
  }

  /**
   * Removes a registration from the list of clients currently connected to
   * this ChatServer instance. This method is synchronized to prevent
   * simultaneous update of the client list.
   * @param rel  The RemoteEventListener implementation to remove.
   */
  protected synchronized void removeClient (RemoteEventListener rel) {
    clients.remove (rel);
    System.out.println ("Removed client : " + rel.toString ());
  }

  // In interface ChatServerInterface

  public void say (String msg) throws java.rmi.RemoteException
  {
    if (msg != null) {
      addMessage (msg);
    }
  }
  
  // In interface ChatServerInterface
  
  public void sendImage (ImageIcon image) throws java.rmi.RemoteException
  {
	  if(image !=  null)
	  {
		  addImage(image);
	  }
  }
  
   // In interface ChatServerInterface
  
  public void sendAud (AudioStream aud) throws java.rmi.RemoteException
  {
	  if(aud !=  null)
	  {
		  addAud(aud);
	  }
  }

  // In interface ChatServerInterface

  public String getName () throws java.rmi.RemoteException {
    return serverName;
  }

  // In interface ChatServerInterface

  public void register (RemoteEventListener rel)
    throws java.rmi.RemoteException
  {
    if (rel != null) {
      addClient (rel);
    }
  }

  // In interface ChatServerInterface

  public void unregister (RemoteEventListener rel)
    throws java.rmi.RemoteException
  {
    if (rel != null) {
      removeClient (rel);
    }
  }

  /**
   * This method is where the delivery thread (in method run()) rests
   * while the message queue is empty.
   */
  protected synchronized void snooze () {
    try {
      wait ();
    }
    catch (java.lang.InterruptedException iex) {}
    catch (java.lang.IllegalMonitorStateException ims) {}
  }

  /**
   * This method is called when the service interface has added a new
   * message to the message queue. If the delivery thread is waiting
   * in snooze(), it will continue as soon as this method is exited.
   * The thread that calls this method is the RMI service thread, the
   * thread that channels remote requests into the service interface code.
   * The call sequence is: say(String):addMessage(String):wakeUp().
   */
  protected synchronized void wakeUp () {
    notify ();
  }

  /**
   * This is where the distribution thread spends its time. It dequeues
   * the message queue, builds a ChatNotification event and sends it to
   * each client that has registered a remote event listener with us.
   * When the message queue is empty, the thread calls snooze() and does
   * nothing until it is awakened by the code that has added a new
   * message to the message queue.
   */
  public void run () {

    while (runDelivery) {
	  if(isAud == 1)
	  {
		  AudioStream aud = getNextAud();
		  if(aud != null)
		  {
			  //Prepare a notification
			  ChatNotification note = new ChatNotification(this,aud,audCount);
			  // Send it to all registered listeners.
			  for (int i = 0; i < clients.size (); i++) {
					try {
						RemoteEventListener rel =
						(RemoteEventListener) clients.elementAt (i);
						rel.notify (note);
					}
					catch (java.lang.ArrayIndexOutOfBoundsException aio) {}
					catch (net.jini.core.event.UnknownEventException uee) {}
					catch (java.rmi.RemoteException rex) {}
				}
		  }
		  else {
			  snooze();
		  }
		  isAud=0;
	  }
	  else if(isImage == 1)
	  {
		  ImageIcon img = getNextImage();
		  if(img != null)
		  {
			  //Prepare a notification
			  ChatNotification note = new ChatNotification(this,img,imgCount);
			  // Send it to all registered listeners.
			  for (int i = 0; i < clients.size (); i++) {
					try {
						RemoteEventListener rel =
						(RemoteEventListener) clients.elementAt (i);
						rel.notify (note);
					}
					catch (java.lang.ArrayIndexOutOfBoundsException aio) {}
					catch (net.jini.core.event.UnknownEventException uee) {}
					catch (java.rmi.RemoteException rex) {}
				}
		  }
		  else {
			  snooze();
		  }
		  isImage=0;
	  }
	  else
	  {
		String msg = getNextMessage ();
		if (msg != null) {
			// Prepare a notification
			ChatNotification note = new ChatNotification (this, msg, msgCount);
			// Send it to all registered listeners.
				for (int i = 0; i < clients.size (); i++) {
					try {
						RemoteEventListener rel =
						(RemoteEventListener) clients.elementAt (i);
						rel.notify (note);
					}
					catch (java.lang.ArrayIndexOutOfBoundsException aio) {}
					catch (net.jini.core.event.UnknownEventException uee) {}
					catch (java.rmi.RemoteException rex) {}
				}
		}
		else {
		snooze ();
		}  
	  }
    } // while runDelivery

    System.out.println ("\nDelivery thread exiting.");
  }

  /**
   * This method implements a small command interpreter which only
   * exists to perform a graceful shutdown of the server.
   */
  public void readLoop () {
    boolean halted = false;
    BufferedReader d = new BufferedReader(new InputStreamReader(System.in));
    System.out.println ("Server " + serverName + " started.");
    while (!halted) {
      System.out.print ("Server> ");
      System.out.flush ();
      String buf = null;
      try {
	buf = d.readLine ();
      }
      catch (java.io.IOException iox) {
	iox.printStackTrace ();
	System.out.println ("\nI/O error in command interface.");
	halted = true;
	continue;
      }

      if (buf == null) {
	halted = true;
	continue;
      }

      String arg = buf.trim ();

      if (arg.length () == 0) {
	continue;
      }

      if (arg.equalsIgnoreCase ("quit") ||
	  arg.equalsIgnoreCase ("stop") ||
	  arg.equalsIgnoreCase ("halt") ||
	  arg.equalsIgnoreCase ("exit")) {
	halted = true;
      }
      else if (arg.equalsIgnoreCase ("help")) {
	System.out.println ("Available commands:");
	System.out.println ("quit      Shuts down the server.");
	System.out.println ("help      This text.");
      }
      else {
	System.out.println ("\nUnknown server command : " + arg);
      }
    }

    System.out.println ("\nShutting down, please wait...");
    runDelivery = false;
    wakeUp ();
    shutdown ();
    System.out.println ("Join manager terminated.");
  }

  /**
   * Contains the help text strings for the commandline interface.
   */
  protected static String [] usageText = new String [] {
    "Usage: [-n server-name]",
    "       [-h|--help]"
  };

  /**
   * This method implements the commandline help command.
   */
  protected static void usage () {
    for (int i = 0; i < usageText.length; i++) {
      System.out.println (usageText[i]);
    }
  }

  // The main program.

  public static void main (String [] argv)
    throws
      java.io.IOException,
      java.rmi.RemoteException,
      java.net.UnknownHostException
  {
    
    String serverName = null;
    int state = 0;

    for (int i = 0; i < argv.length; i++) {
      String av = argv[i];
      if (state == 0) {
	if (av.equalsIgnoreCase ("-n")) {
	  state = 1;
	}
	else if (av.equalsIgnoreCase ("-h") ||
		 av.equalsIgnoreCase ("--help")) {
	  usage ();
	  System.exit (0);
	}
	else {
	  usage ();
	  System.exit (1);
	}
      }
      else if (state == 1) {
	serverName = av;
	state = 0;
      }
    }

    System.setSecurityManager (new RMISecurityManager ());
    ChatServer cs = new ChatServer (serverName);
    cs.readLoop ();
    System.exit (0);
  }
}
