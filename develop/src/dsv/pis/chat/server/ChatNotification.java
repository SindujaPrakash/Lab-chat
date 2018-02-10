// ChatNotification.java
// Fredrik Kilander, DSV SU/KTH
// 18-mar-2004/FK First version

package dsv.pis.chat.server;

import net.jini.core.event.RemoteEvent;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
//import java.applet.*;
import sun.audio.*;

/**
 * This class implements the notification that is sent to the ChatClients
 * as a new piece of text in the discussion is distributed. The clients
 * can obtain the serial number by calling method getSequenceNumber ()
 * (defined in the superclass RemoteEvent) and the message text by calling
 * method getText () defined below.
 */
public class ChatNotification
  extends
    RemoteEvent
{

  /**
   * The text of the message.
   */
  protected String text;
  protected ImageIcon image;
  protected AudioStream aud;
  public int isImage = 0;
  public int isAud = 0;

  /**
   * Creates a new ChatNotification instance.
   * @param source The object from which this instance originates.
   * @param msg    The message to the client.
   * @param serial The serial number of the message in the server's sequence.
   */
  public ChatNotification (Object source, String msg, int serial) {
    // Call the constructor of the superclass (RemoteEvent) explicitly
    // so that its fields can be initialized to what we want. Actually,
    // we are only putting the serial number in as the sequence nr, but
    // the other arguments could be there as well if we had use for them.
    super (source,		// Source
	   0,			// ID
	   serial,		// sequence number
	   null);		// handback
    this.text = msg;
  }
   
    /**
   * Creates a new ChatNotification instance.
   * @param source The object from which this instance originates.
   * @param aud    The audio to the client.
   * @param serial The serial number of the message in the server's sequence.
   */
   
   public ChatNotification (Object source, ImageIcon img, int serial) {
    // Call the constructor of the superclass (RemoteEvent) explicitly
    // so that its fields can be initialized to what we want. Actually,
    // we are only putting the serial number in as the sequence nr, but
    // the other arguments could be there as well if we had use for them.
    super (source,		// Source
	   0,			// ID
	   serial,		// sequence number
	   null);		// handback
    this.image = img;
	isImage = 1;
  }
  
    /**
   * Creates a new ChatNotification instance.
   * @param source The object from which this instance originates.
   * @param img    The message to the client.
   * @param serial The serial number of the message in the server's sequence.
   */
   
   public ChatNotification (Object source, AudioStream audio, int serial) {
    // Call the constructor of the superclass (RemoteEvent) explicitly
    // so that its fields can be initialized to what we want. Actually,
    // we are only putting the serial number in as the sequence nr, but
    // the other arguments could be there as well if we had use for them.
    super (source,		// Source
	   0,			// ID
	   serial,		// sequence number
	   null);		// handback
    this.aud = audio;
	isAud = 1;
  }
  

  /**
   * Returns the text message in the notification.
   * @return The text message.
   */
  public String getText () {
    return text;
  }
  
  /**
   * Returns the image in the notification.
   * @return The image.
   */
  public ImageIcon getImage () {
    return image;
  }
  
  /**
   * Returns the audio in the notification.
   * @return The audio.
   */
  public AudioStream  getAud () {
    return aud;
  }
}
