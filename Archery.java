import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class Archery extends JPanel implements Runnable {
	
	//main thread
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame();
				frame.setTitle("Archery | JW™");
				frame.setDefaultCloseOperation(3);
				
				Archery gamePanel = new Archery();
				frame.add(gamePanel);
				
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}
	
	//fields
	private final int FPS = 175;
	
	//game objects
	private Vector mousePosition = new Vector(); //TODO: so there isn't an ugly path line at the beginning
	private boolean mouseLeftPressed;
	private boolean mouseRightPressed;
	
	private Vector initPosition = new Vector();
	private Vector endPosition = new Vector();
	
	private int power;
	
	private Vector gravity = new Vector(0, 9.81 / FPS); 
	private boolean precisionLine = true;
	private boolean precisionLineOnlyMouse = false;
	private boolean blockSolidWhileInPermeableState = true; //true: collision detections also while moving the block
	private boolean antiAliasing = true;
	private boolean maxForce = true;
	
	private ArrayList<Arrow> arrows = new ArrayList<>();
	private Block block = null; //TODO: collisionsabfrage nur noch wenn status block verändert; pfeile immer ganz an block ran
							   //TODO: pfeile verlieren geschw. bei kollision
	
	//constructor
	private Archery() {
		super();
		
		setFocusable(true);
		requestFocusInWindow();
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					initPosition.setX(e.getX());
					initPosition.setY(e.getY());
					
					mouseLeftPressed = true;
				} else if(e.getButton() == MouseEvent.BUTTON3) {
					mouseRightPressed = true;
					
					block.setState(Block.State.PERMEABLE);
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					mouseLeftPressed = false;
					
					endPosition.setX(e.getX());
					endPosition.setY(e.getY());
					
					if(power > 0)
						createNewArrow();
				} else if(e.getButton() == MouseEvent.BUTTON3) {
					mouseRightPressed = false;
					
					block.setState(Block.State.SOLID);
				}
			}
		});
		
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				mousePosition.setX(e.getX());
				mousePosition.setY(e.getY());
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				mousePosition.setX(e.getX());
				mousePosition.setY(e.getY());
			}
		});
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keycode = e.getKeyCode();
				
				if(keycode == KeyEvent.VK_I) { //invert gravity
					gravity.multiply(-1);
				} 
				else if(keycode == KeyEvent.VK_G) { //switch gravity on and off
					if(gravity.getBetrag() > 0)
						gravity.setY(0);
					else
						gravity.setY(9.81 / FPS);
				} 
				else if(keycode == KeyEvent.VK_P) {
					precisionLine = !precisionLine;
				} 
				else if(keycode == KeyEvent.VK_O) {
					precisionLineOnlyMouse = !precisionLineOnlyMouse;
				} 
				else if(keycode == KeyEvent.VK_B) {
					blockSolidWhileInPermeableState = !blockSolidWhileInPermeableState;
				}
				else if(keycode == KeyEvent.VK_A) {
					antiAliasing = !antiAliasing;
				}
				else if(keycode == KeyEvent.VK_SPACE) {
					createArrowBomb();
				}
				else if(keycode == KeyEvent.VK_M) {
					maxForce = !maxForce;
				}
			}
		});
	}
	
	//methods
	
	@Override
	public Dimension getPreferredSize() {
		return Toolkit.getDefaultToolkit().getScreenSize();
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		
		new Thread(this).start();
	}
	
	//rendering
	@Override
	public void paintComponent(Graphics g1) {
		super.paintComponent(g1);
		
		Graphics2D g = (Graphics2D) g1;
		if(antiAliasing)
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		//clearing screen
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		//render entities
		
		//render arrows
		for(int i = arrows.size() - 1; i >= 0; i--) {
			arrows.get(i).draw(g); //liefert manchmal eventqueue fehler, da paintComponent() genutzt wird
		}
		block.draw(g);
	
		
		//draw mouseline
		if(mouseLeftPressed) {
			
			g.setColor(Color.ORANGE);
			g.setStroke(new BasicStroke(3f));
			g.drawLine((int) initPosition.getX(),(int) initPosition.getY(),
					   (int)mousePosition.getX(),(int) mousePosition.getY());
			g.drawString("Power: " + power, (int) mousePosition.getX(), (int) mousePosition.getY());
		}
		
		//fluglinie zeichnen
		out:
		if(precisionLine) {
			if(precisionLineOnlyMouse) {
				if(!mouseLeftPressed)
					break out;
			}
			g.setColor(Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(3f));
			
			Vector power = (mouseLeftPressed) ? initPosition.getSubtraction(mousePosition) : initPosition.getSubtraction(endPosition);
			power.multiply(0.1);
			Vector nextPos = initPosition.getClone();
			for(int i = 0; i < 1000; i++) { //nächsten 100 positionen
				//pos bestimmen
				power.add(gravity);
				nextPos.add(power);
				//zeichnen
				g.drawLine((int) nextPos.getX(), (int) nextPos.getY(), (int) nextPos.getX(), (int) nextPos.getY());
				
			}
		}
		
		//rendering control information
		g.setColor(Color.DARK_GRAY);
		g.drawString("Number of arrows: " + arrows.size(), getWidth() - 300, getHeight() - 60);
		g.drawString("Move block by pressing right mouse button", getWidth() - 300, getHeight() - 80);
		
		g.drawString("Arrow Bomb (Space)", 10, getHeight() - 160); 
		g.drawString("Max Force (M)", 10, getHeight() - 140);
		g.drawString("Pressed mouse path only (O)", 10, getHeight() - 120); 
		g.drawString("Show path (P)", 10, getHeight() - 100); 
		g.drawString("Invert gravity (I)", 10, getHeight() - 80);
		g.drawString("Gravity (G)", 10, getHeight() - 60);
		g.drawString("Block solid while moving (B)", 10, getHeight() - 40);
		g.drawString("Anti Aliasing (A)", 10, getHeight() - 20); 
		
	}
	
	
	//gameloop
	@Override
	public void run() {
		int delta = (int) Math.ceil(1000. / FPS);
		
		block = new Block(300, 300, 100, 100);
		
		while(isVisible()) {
			try {
				Thread.sleep(delta);
			} catch (InterruptedException e) {}
			
			//update
			update();
			//new frame
			repaint();
		}
	}

	private void update() {
		if(mouseLeftPressed) {
			//compute power
			Vector power = initPosition.getSubtraction(mousePosition);
			this.power = (int) power.getBetrag();		
		}
		if(block.getState() == Block.State.PERMEABLE) {
			block.setCoordinates((int) mousePosition.getX(), (int) mousePosition.getY());
		}
		
		//decide which arrows need to be removed because of performance reasons
		for(int i = arrows.size() - 1; i >= 0; i--) {
			Arrow a = arrows.get(i);
			if(a.pos.getX() < -100 || a.pos.getX() > getWidth() + 100 || a.pos.getY() < -100 || a.pos.getY() > getHeight() + 100) {
				arrows.remove(a);
			}
		}
		
		//update arrows
		for(Arrow a : arrows) {
			a.update();
		}
		block.update();
	}
	
	//called when mouse is released
	private void createNewArrow() {
		Vector power = initPosition.getSubtraction(mousePosition);
		power.multiply(0.1); //Damit nicht so schnell 0.1
		arrows.add(new Arrow(initPosition.getClone(), power)); 
	}
	//when space is pressed
	private void createArrowBomb() {
		for(int i = 0; i < 30; i++) {
			Vector power = new Vector(Math.sin(i), Math.cos(i));
			power.multiply(1/power.getBetrag()); //Damit nicht so schnell 0.1
			power.multiply(7);
			arrows.add(new Arrow(initPosition.getClone(), power));
		}
	}
	
	private class Arrow implements Drawable, Updateable {
		private Vector pos;
		private Vector vel;
		
		private int r = 10 ; //radius
		
		Arrow(Vector pos, Vector vel) {
			this.pos = pos;
			this.vel = vel;
		}
		
		public void draw(Graphics2D g) {
			g.setColor(Color.RED);
			g.fillOval((int)pos.getX() - r, (int)pos.getY() - r, r*2, r*2);
			
			//vel vektor zeichnen
			//g.setColor(Color.DARK_GRAY);
			//g.drawLine((int)pos.getX(), (int)pos.getY(), (int)pos.getX()  + (int)vel.getX() * 10, (int)pos.getY() + (int)vel.getY() * 10);
			
			//pfeilrest zeichnen
			Vector rest = vel.getClone();
			rest.multiply(1/rest.getBetrag());
			rest.multiply(35);
			rest.multiply(-1);
			g.setColor(Color.DARK_GRAY);
			g.setStroke(new BasicStroke(3f));
			g.drawLine((int)pos.getX(), (int)pos.getY(), (int)pos.getX()  + (int)rest.getX(), (int)pos.getY() + (int)rest.getY());
		}
		
		public void update() {
			//adapt vel
			vel.add(gravity);
			pos.add(vel);
			
			if(collidesWith(block)) {
				pos.subtract(vel);
				vel.subtract(gravity);
				//vel.setX(0);
				//vel.setY(0);
			}
		}
		
		private boolean collidesWith(Block b) {
			if(!blockSolidWhileInPermeableState && block.getState() == Block.State.PERMEABLE)
				return false;
				
			if(MyMath.inRange(pos.getX(), b.getX1(), b.getX2()) && MyMath.inRange(pos.getY(), b.getY1(), b.getY2()))
					return true;
			return false;
		}
	}
	
	private static class Block implements Drawable, Updateable {
		private int x1, y1, width, height;
		private State state = State.SOLID;
		
		private static enum State {
			SOLID, PERMEABLE
		}
		
		Block(int x1, int y1, int width, int height) {
			this.x1 = x1;
			this.y1 = y1;
			this.width =  width;
			this.height = height;
		}

		@Override
		public void update() {
			
		}

		@Override
		public void draw(Graphics2D g) {
			g.setColor(Color.DARK_GRAY);
			g.fillRect(x1, y1, width, height);
		}
		
		//setters and getters
		void setState(State s) {
			state = s;
		}
		State getState() {
			return state;
		}
		
		public int getX1() {return x1;}
		public int getY1() {return y1;}
		
		public int getX2() {return x1 + width;}
		public int getY2() {return y1 + height;}
		
		void setCoordinates(int x, int y) {
			x1 = x; 
			y1 = y;
		}
	}
	
	//base code
	private interface Drawable {
		void draw(Graphics2D g);
	}
	
	private interface Updateable {
		void update();
	}
	
	
	private class Vector {
		//fields
		private double x, y; //coordinates
		
		public Vector(double x, double y) {
			this.x = x;
			this.y = y;
		}
		public Vector() {
			this(0, 0);
		}
		
		//methods
		public void add(Vector b) {
			x += b.x;
			y += b.y;
		}
		public void subtract(Vector b) {
			x -= b.x;
			y -= b.y;
		}
		
		public Vector getSubtraction(Vector b) {
			return new Vector(x - b.x, y - b.y);
		}
		public void multiply(double d) {
			x *= d;
			y *= d;
		}
		public double getBetrag() {
			return Math.sqrt(x*x + y*y);
		}
		
		@Override
		public String toString() {
			return "[" + x + "|" + y + "]";
		}
		
		//setters
		synchronized void setX(double i) {
			x = i;
		}
		
		synchronized void setY(double i) {
			y = i;
		}
		
		//getters
		synchronized double getX() {
			return x;
		}
		synchronized double getY() {
			return y;
		}
		
		public Vector getClone() {
			return new Vector(x, y);
		}
	}
	
	private static class MyMath {
		static boolean inRange(double value, double min, double max) {
				return (value >= min && value <= max);
		}
	}
}
