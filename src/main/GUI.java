package main;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.jxta.exception.PeerGroupException;
import api.functions;

public class GUI implements MouseListener, ActionListener, WindowListener {

	JFrame f;
	JButton start, exit;

	JButton ls, cd, mkdir, rm, ltor, rtol;
	JTextField cdt, mkdirt, rmt, ltort1, ltort2, rtolt1, rtolt2;

	JTextArea text;

	functions fs = null;

	public GUI() {
		this.f = new JFrame("Brihaspati GUI");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.start = new JButton("START PEER");
		this.exit = new JButton("EXIT PEER");
		this.ls = new JButton("ls");
		this.cd = new JButton("cd");
		this.cdt = new JTextField();
		this.mkdir = new JButton("mkdir");
		this.mkdirt = new JTextField();
		this.rm = new JButton("rm");
		this.rmt = new JTextField();
		this.ltor = new JButton("ltor");
		this.ltort1 = new JTextField();
		this.ltort2 = new JTextField();
		this.rtol = new JButton("rtol");
		this.rtolt1 = new JTextField();
		this.rtolt2 = new JTextField();
		this.text = new JTextArea();

		this.f.add(this.start);
		this.f.add(this.exit);
		this.f.add(this.ls);
		this.f.add(this.cd);
		this.f.add(this.cdt);
		this.f.add(this.mkdir);
		this.f.add(this.mkdirt);
		this.f.add(this.rm);
		this.f.add(this.rmt);
		this.f.add(this.ltor);
		this.f.add(this.ltort1);
		this.f.add(this.ltort2);
		this.f.add(this.rtol);
		this.f.add(this.rtolt1);
		this.f.add(this.rtolt2);
		this.f.add(this.text);

		this.f.getContentPane().setBackground(new Color(51, 117, 153));
		this.f.setLayout(null);
		this.f.setVisible(true);
		this.f.setSize(800, 600);

		this.start.setBounds(150, 30, 150, 40);
		this.exit.setBounds(550, 30, 150, 40);
		this.start.addActionListener(this);
		this.exit.addActionListener(this);

		int bWidth = 80;
		int bHeight = 30;
		int tWidth = 130;
		int tHeight = 30;
		int row1 = 120;
		int row2 = 200;
		int row3 = 270;
		this.ls.setBounds(330, row1, 140, bHeight);
		this.cdt.setBounds(40 + 10, row2, tWidth, tHeight);
		this.cd.setBounds(40 + 150, row2, bWidth, bHeight);
		this.mkdirt.setBounds(280 + 10, row2, tWidth, tHeight);
		this.mkdir.setBounds(280 + 150, row2, bWidth, bHeight);
		this.rmt.setBounds(520 + 10, row2, tWidth, tHeight);
		this.rm.setBounds(520 + 150, row2, bWidth, bHeight);
		this.ltort1.setBounds(40 + 10, row3, tWidth - 30, tHeight);
		this.ltort2.setBounds(40 + 120, row3, tWidth, tHeight);
		this.ltor.setBounds(40 + 120 + 140, row3, bWidth + 10, bHeight);
		this.rtolt1.setBounds(400 + 10, row3, tWidth - 30, tHeight);
		this.rtolt2.setBounds(400 + 120, row3, tWidth, tHeight);
		this.rtol.setBounds(400 + 120 + 140, row3, bWidth + 10, bHeight);
		this.ls.addActionListener(this);
		this.cd.addActionListener(this);
		this.mkdir.addActionListener(this);
		this.rm.addActionListener(this);
		this.ltor.addActionListener(this);
		this.rtol.addActionListener(this);

		this.text.setEditable(false);
		this.text.setBounds(100, 350, 600, 200);
	}

	public void actionPerformed(ActionEvent ae) {
		this.text.setText(null); // TODO: Why doesn't this work here?

		if (ae.getSource() == this.start) {
			this.text.setText("Starting peer...");
			try {
				fs = new functions();
			} catch (PeerGroupException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				this.text.setText("Peer up and running");
			}

			fs.setPWD("/");
		}
		if (ae.getSource() == this.exit) {
			System.out.println("shutting down peer...");
			this.text.setText("Peer shut down");
			System.exit(0);
		}
		if (ae.getSource() == this.ls) {
			try {
				this.text.setText(fs.ls());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (ae.getSource() == this.cd) {
			String cd_str = this.cdt.getText();
			fs.cd(cd_str);
			try {
				this.text.setText(fs.ls());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (ae.getSource() == this.mkdir) {
			String mkdir_str = this.mkdirt.getText();
			try {
				fs.mkdir(mkdir_str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (ae.getSource() == this.rm) {
			String rm_str = this.rmt.getText();
			try {
				fs.rm(rm_str);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (ae.getSource() == this.ltor) {
			String ltor_fname = this.ltort1.getText();
			String ltor_fpath = this.ltort2.getText();
			try {
				fs.ltor(ltor_fname, ltor_fpath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (ae.getSource() == this.rtol) {
			String rtol_fname = this.rtolt1.getText();
			String rtol_fpath = this.rtolt2.getText();
			fs.rtol(rtol_fname, rtol_fpath);
		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {

	}

	@Override
	public void windowClosed(WindowEvent arg0) {

	}

	@Override
	public void windowClosing(WindowEvent arg0) {

	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {

	}

	@Override
	public void windowIconified(WindowEvent arg0) {

	}

	@Override
	public void windowOpened(WindowEvent arg0) {

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {

	}

	@Override
	public void mouseExited(MouseEvent arg0) {

	}

	@Override
	public void mousePressed(MouseEvent arg0) {

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

	}

	public static void main(String[] args) {
		new GUI();
	}
}
