package client.outils.graphiques;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import client.listeners.GenericMouseListener;
import client.listeners.GenericMouseMotionListener;
import client.listeners.Listeners;
import client.partie.graphique.GameHandler;
import client.partie.graphique.LogWriter;
import client.partie.graphique.RessourceManager;
import client.partie.graphique.RoomType;
import client.room1.Room1Handler;
import client.roomInternet.RoomInternetHandler;
import client.roomReseauLocalAttente.RoomReseauLocalAttenteHandler;
//import commun.partie.nonGraphique.PylosPartie;
import commun.partie.nonGraphique.ModeDeJeu;
import commun.partie.nonGraphique.TeamType;

/** Classe s'occupant de la fenêtre de l'application
 * D'autres classes, comme GameHandler, dessinent dans et via GraphicsHandler.
 *
 */
@SuppressWarnings("serial") // cet objet ne sera de toute façon pas serialisé
public class GraphicsHandler extends Canvas {
	
	/** La stratégie permettant d'avoir un double buffer (page flipping) */
	private BufferStrategy currentBufferStrategy;
	static public final int windowWidth = 1000;
	static public final int windowHeight = 680;
	
	public AtomicBoolean gameRunning = new AtomicBoolean(true);
	private Graphics2D currentGraphics = null;
	private static GraphicsHandler mainInstance = null; // une seule instance possible
	
	public static RoomType currentRoomType = RoomType.AUCUN;
	public static String currentRoomName = "Pas de nom !";
	
	public GraphicsHandler() throws Exception {
		if (mainInstance != null) {
			System.out.println("GraphicsHandler : constructeur : une instance de GraphicsHandler existe déjà. Elle doit être unique.");
			throw new Exception("GraphicsHandler : constructeur : une instance de GraphicsHandler existe déjà. Elle doit être unique.");
		}
		mainInstance = this;
		initWindow();
	}
	
	public static GraphicsHandler getMainInstance() {
		return mainInstance;
	}
	public static Graphics2D getMainGraphics() {
		return (Graphics2D) mainInstance.currentBufferStrategy.getDrawGraphics();
	}
	
	JTextField exempleTextField = new JTextField("--> Nouveau texte à modifier <--");
	JPasswordField exempleTextField4 = new JPasswordField("--> Nouveau texte à modifier <--");
	
	
	
	//JTextField myJt;
	
	private static JPanel applicationPanel;
	
	public void initWindow() {
		JFrame container = new JFrame("Jeu de pylos en java");
		JPanel panel = (JPanel) container.getContentPane(); // get hold the content of the frame and set up the resolution of the game
		applicationPanel = panel;
		panel.setPreferredSize(new Dimension(windowWidth, windowHeight));
		
		
		panel.setLayout(null);
		setBounds(0, 0, windowWidth, windowHeight); // setup our canvas size and put it into the content of the frame
		Color col = new Color(236, 232, 230);
		
		exempleTextField.setBackground(col);
		//exempleTextField.setSelectedTextColor(Color.RED);
		exempleTextField.setForeground(Color.black);
		exempleTextField.setBorder(null);
		Font ft = exempleTextField.getFont();
		
		RoomReseauLocalAttenteHandler.addOnceToPanel(panel);
		RoomReseauLocalAttenteHandler.setVisibleFields(true);
		//exempleTextField.setFont();
		/*exempleTextField.setBounds(500, 100, 300, 30);
		exempleTextField4.setBounds(300, 200, 300, 30);
		panel.add(exempleTextField);
		panel.add(exempleTextField4);
		exempleTextField.setVisible(false);
		exempleTextField4.setVisible(false);*/
		
		panel.add(this);
		setIgnoreRepaint(false); // Tell AWT not to bother repainting our canvas since we're going to do that ourself in accelerated mode
		container.pack(); // finally make the window visible 
		container.setResizable(false);
		container.setVisible(true);
		// add a listener to respond to the user closing the window. If they do we'd like to exit the game
		container.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		//myJt = new JTextField("Mon test !");
		//myJt.setBounds(50, 100, 200, 30);
		//panel.add(myJt);
		//myJt.repaint();
		
		// create the buffering strategy which will allow AWT to manage our accelerated graphics
		createBufferStrategy(2);
		currentBufferStrategy = getBufferStrategy();
		LogWriter.Log("GameHandler.InitWindow : currentPath = " + System.getProperty("user.dir"));
		// Centrer la fenêtre
		container.setLocationRelativeTo(null);
		
		// Ajout de l'écoute de la souris
		GraphicsHandler ginstance = this;// = GraphicsHandler.getMainInstance();
		ginstance.addMouseListener(new GenericMouseListener());
		ginstance.addMouseMotionListener(new GenericMouseMotionListener());
	}
	
	
	public void loop() {
		long lastLoopTime = System.currentTimeMillis();
		
		// test -> new Thread(new AsynchronousBu()).start();
		
		// keep looping round till the game ends
		while (gameRunning.get()) {
			// work out how long its been since the last update, this will be used to calculate how far the entities should move this loop
			long delta = System.currentTimeMillis() - lastLoopTime; // temps écoulé depuis la dernière itération
			lastLoopTime = System.currentTimeMillis();
			// Get hold of a graphics context for the accelerated surface and blank it out
			currentGraphics = getMainGraphics();//(Graphics2D) currentBufferStrategy.getDrawGraphics();
			currentGraphics.setColor(Color.darkGray);
			currentGraphics.fillRect(0, 0, windowWidth, windowHeight);
			
			Point mousePos = MouseInfo.getPointerInfo().getLocation();
			// (débug) dessin de la position de la souris
			Color col = new Color(200, 20, 200);
			currentGraphics.setColor(col);
			currentGraphics.drawString("Mouse pos : " + mousePos.x + ", " + mousePos.y, 10, 100);
			drawRoomName();
			
			// Dessin du bon type d'écran
			if (currentRoomType == RoomType.PARTIE) {
				GameHandler.staticGameLoop();
				drawReturnButton(); // l'état de la souris est utile pour 
				Listeners.frame_clearMouseSate(); // ne supprime pas les évènements, réinitialise juste l'état de la souris pour la frame suivante
				// pas de Listeners.clearEvents(); ici, c'est GameHandler qui s'en charge !
			}
			if (currentRoomType == RoomType.MENU_CHOIX_TYPE_PARTIE) {
				Listeners.refreshFrameListenerEvents(); // pour faire la détection des collisions en même temps que l'affichage graphique
				Room1Handler.staticLoop();
				// surtout pas de Listeners.clearEvents(); c'est refreshFrameListenerEvents() qui s'en charge !
				// seulement pour GameHandler : Listeners.frame_clearMouseSate();
			}

			if (currentRoomType == RoomType.MENU_RESEAU_LOCAL) {
				Listeners.refreshFrameListenerEvents(); // pour faire la détection des collisions en même temps que l'affichage graphique
				RoomReseauLocalAttenteHandler.staticLoop();
				drawReturnButton(); // l'état de la souris est utile pour 
				// surtout pas de Listeners.clearEvents(); c'est refreshFrameListenerEvents() qui s'en charge !
				// seulement pour GameHandler : Listeners.frame_clearMouseSate();
			}
			
			if (currentRoomType == RoomType.MENU_INTERNET) {
				Listeners.refreshFrameListenerEvents(); // pour faire la détection des collisions en même temps que l'affichage graphique
				RoomInternetHandler.staticLoop();
				drawReturnButton(); // l'état de la souris est utile pour 
				// surtout pas de Listeners.clearEvents(); c'est refreshFrameListenerEvents() qui s'en charge !
				// seulement pour GameHandler : Listeners.frame_clearMouseSate();
			}
			
			//myJt.repaint();
			//myJt.paint(currentGraphics);
			
			
			// finally, we've completed drawing so clear up the graphics
			// and flip the buffer over
			currentGraphics.dispose();
			currentBufferStrategy.show();
			
			// finally pause for a bit. Note: this should run us at about
			// 100 fps but on windows this might vary each loop due to
			// a bad implementation of timer
			try { Thread.sleep(16); } catch (Exception e) { }
			
			
		}
	}
	
	public static void roomGoTo_game(ModeDeJeu modeDeJeu, TeamType cEstLeTourDe, TeamType equipeJoueurActuel) {
		Listeners.clearEvents();
		RoomReseauLocalAttenteHandler.setVisibleFields(false);
		// stopper le serveur local si :
		if (  (modeDeJeu != ModeDeJeu.RESEAU_LOCAL) // la partie suivante n'est pas en local
		   || (modeDeJeu == ModeDeJeu.RESEAU_LOCAL && RoomReseauLocalAttenteHandler.estLeServeur == false)) { // la partie est en réseau local mais je ne suis pas le serveur
				RoomReseauLocalAttenteHandler.stopLocalServer(); // stopper le serveur local
		}
		if (modeDeJeu != ModeDeJeu.INTERNET) {
			RoomInternetHandler.reinitialiserTout(false);
		}
		
		currentRoomType = RoomType.PARTIE;
		//GameHandler game = 
		new GameHandler(modeDeJeu, cEstLeTourDe, equipeJoueurActuel);
	}
	
	public static void roomGoTo_menuChoixTypePartie() {
		Listeners.clearEvents();
		RoomReseauLocalAttenteHandler.setVisibleFields(false);
		RoomReseauLocalAttenteHandler.stopLocalServer();
		RoomReseauLocalAttenteHandler.closeAutreJoueurLocalTCPClient();
		RoomInternetHandler.reinitialiserTout(false);
		currentRoomType = RoomType.MENU_CHOIX_TYPE_PARTIE;
		//Room1Handler room1Handler = 
		new Room1Handler();
	}
	
	public static void roomGoTo_menuReseauLocal() {
		Listeners.clearEvents();
		RoomReseauLocalAttenteHandler.stopLocalServer();
		RoomReseauLocalAttenteHandler.closeAutreJoueurLocalTCPClient();
		RoomInternetHandler.reinitialiserTout(false);
		currentRoomType = RoomType.MENU_RESEAU_LOCAL;
		RoomReseauLocalAttenteHandler.setVisibleFields(true);
		RoomReseauLocalAttenteHandler.startLocalServer();
	}
	
	public static void roomGoTo_internet() {
		Listeners.clearEvents();
		RoomReseauLocalAttenteHandler.setVisibleFields(false);
		RoomReseauLocalAttenteHandler.stopLocalServer();
		RoomReseauLocalAttenteHandler.closeAutreJoueurLocalTCPClient();
		RoomInternetHandler.reinitialiserTout(true);
		RoomInternetHandler.loadImages();
		currentRoomType = RoomType.MENU_INTERNET;
	}
	
	public static void drawReturnButton() {
		if (mainInstance == null) return;
		int xMouse = Listeners.frame_getMouseX();
		int yMouse = Listeners.frame_getMouseY();
		Image spr_Bouton_retour = RessourceManager.LoadImage("images/Bouton_retour.png");
		// Dessin du bouton retour
		int xBoutonRetour = 10;
		int yBoutonRetour = 10;
		int boutonRetourWidth = PImage.getImageWidth(spr_Bouton_retour);
		int boutonRetourHeight = PImage.getImageHeight(spr_Bouton_retour);
		BoxPosition box = new BoxPosition(xBoutonRetour, yBoutonRetour, xBoutonRetour + boutonRetourWidth, yBoutonRetour + boutonRetourHeight);
		int posOffset = 0;
		boolean changeRoom = false;
		if (box.isInside(xMouse,  yMouse)) { // souris
			posOffset = 3;
			double colorFactor = 0.1;
			PImage.drawImageColorAlpha(mainInstance.currentGraphics, spr_Bouton_retour, xBoutonRetour, yBoutonRetour, colorFactor, colorFactor, colorFactor, 1);
			if (Listeners.frame_mouseReleased()) {
				changeRoom = true;
			}
		}
		PImage.drawImageAlpha(mainInstance.currentGraphics, spr_Bouton_retour, xBoutonRetour - posOffset, yBoutonRetour - posOffset, 1);
		if (changeRoom) {
			switch (currentRoomType) {
			case MENU_CHOIX_TYPE_PARTIE : break;
			case PARTIE :
				if (GameHandler.partieActuelle != null) { // partie actuelle doit être non nulle, normalement.
					if (GameHandler.partieActuelle.modeDeJeu != ModeDeJeu.INTERNET) { // pas jeu sur internet : je retourne à la sélection des parties
						roomGoTo_menuChoixTypePartie();
					} else { // jeu sur internet : je retourne à la salle internet, j'interromps la partie actuelle avec mon adversaire (avec malus de points etc.)
						RoomInternetHandler.quitterPartieEnCours();
						roomGoTo_internet();
					}
				} else { // ne deverait jamais arriver normalement, que partieActuelle == null
					roomGoTo_menuChoixTypePartie();
				}
				break;
			case MENU_RESEAU_LOCAL : roomGoTo_menuChoixTypePartie(); break;
			case MENU_INTERNET : roomGoTo_menuChoixTypePartie(); break;
			}
		}
		//
		
	}
	private void drawRoomName() {
		
		String roomNameAsString = "salle inconnue";
		switch (currentRoomType) {
		case PARTIE :
			if (GameHandler.partieActuelle != null) {
				switch (GameHandler.partieActuelle.modeDeJeu) {
				case SOLO_LOCAL :
					roomNameAsString = "Partie en solo contre IA";
					break;
				case HOT_SEAT :
					roomNameAsString = "Partie en hot seat";
					break;
				case RESEAU_LOCAL :
					roomNameAsString = "Partie en réseau local";
					break;
				case INTERNET :
					roomNameAsString = "Partie sur Internet !";
					break;
				}
			}
			break;
		case MENU_CHOIX_TYPE_PARTIE :
			roomNameAsString = "Choix du type de partie";
			break;
		case MENU_RESEAU_LOCAL :
			roomNameAsString = "Jeu en réseau local";
			break;
		case MENU_INTERNET :
			roomNameAsString = "Jeu sur internet";
			break;
		default : 
			break;
		}
		
		
		// currentRoomName  currentGraphics
		if (currentGraphics == null) return;
		int xRoomName = 250;
		int yRoomName = 50;
		currentGraphics.setFont(new Font("TimesRoman", Font.PLAIN, 40)); 
		currentGraphics.drawString(roomNameAsString, xRoomName, yRoomName);
		//currentRoomName
		
		
		
		
		
	}
	
	private void importFonts() {
		/*try {
		     GraphicsEnvironment ge =  GraphicsEnvironment.getLocalGraphicsEnvironment();
		     ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("path/to/your/font/sampleFont.ttf"));
		} catch (IOException|FontFormatException e) {
		     //Handle exception
		}*/
	}
	
}
