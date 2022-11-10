import java.awt.EventQueue;
import java.awt.Graphics;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class WordChainGameClientRoomView extends JFrame{

	private JPanel contentPanel;
	
	//image
	private Image background = new ImageIcon("images/background1.png").getImage();
	private Image scoreBackground = new ImageIcon("images/score.png").getImage();
	private Image timerBackground = new ImageIcon("images/timer.png").getImage();
	private Image resizeBackground = timerBackground.getScaledInstance(120, 120,Image.SCALE_SMOOTH);
	
	private ImageIcon happyEmo = new ImageIcon("images/emoticon_happy.png");
	private ImageIcon loveEmo = new ImageIcon("images/emoticon_love.png");
	private ImageIcon supriseEmo = new ImageIcon("images/emoticon_suprise.png");
	private ImageIcon sleepEmo = new ImageIcon("images/emoticon_sleep.png");
	private ImageIcon AngryEmo = new ImageIcon("images/emoticon_angry.png");
	
	private JTextField textField;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WordChainGameClientRoomView window = new WordChainGameClientRoomView();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WordChainGameClientRoomView() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		setTitle("kkutu");
		setSize(700, 700);
		setResizable(false);
		setLocationRelativeTo(null); 
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//content panel에 이미지 추가
		contentPanel = new JPanel() {
			public void paintComponent(Graphics g) {
                g.drawImage(background, 0, 0, this.getWidth(), this.getHeight(), null);
                setOpaque(false);
                super.paintComponent(g);
            }
		};
		contentPanel.setBorder(new EmptyBorder(5, 5, 5 ,5));
		setContentPane(contentPanel);
		contentPanel.setLayout(null);
		
		JScrollPane chattingScrollPane = new JScrollPane();
		chattingScrollPane.setBounds(6, 448, 688, 178);
		contentPanel.add(chattingScrollPane);
		
		JLabel lblNewLabel = new JLabel("채팅");
		chattingScrollPane.setColumnHeaderView(lblNewLabel);
		
		textField = new JTextField();
		textField.setBounds(6, 628, 556, 38);
		contentPanel.add(textField);
		textField.setColumns(10);
		
		JButton btnNewButton = new JButton("전송");
		btnNewButton.setBounds(574, 628, 120, 35);
		contentPanel.add(btnNewButton);
		
		JLabel roomNameLabel = new JLabel("RoomName");
		roomNameLabel.setBounds(17, 21, 135, 16);
		contentPanel.add(roomNameLabel);
		
		
		JLabel peopleLabel = new JLabel("참가인원수");
		peopleLabel.setBounds(545, 21, 86, 16);
		contentPanel.add(peopleLabel);
		
		JLabel countLabel = new JLabel("60초");
		countLabel.setBounds(643, 21, 38, 16);
		contentPanel.add(countLabel);
		
		JLabel gameTypeLabel = new JLabel("게임종류");
		gameTypeLabel.setBounds(431, 21, 61, 16);
		contentPanel.add(gameTypeLabel);
		
		JPanel scorePanel = new JPanel() {
			public void paintComponent(Graphics g) {
                g.drawImage(scoreBackground, 0, 0, this.getWidth(), this.getHeight(), null);
                setOpaque(false);
                super.paintComponent(g);
            }
		};
		scorePanel.setBounds(78, 78, 417, 113);
		scorePanel.setLayout(null);
		contentPanel.add(scorePanel);
		
		JLabel wordLabel = new JLabel("단어");
		wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
		wordLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 30));
		wordLabel.setForeground(new Color(251, 255, 250));
		wordLabel.setBounds(6, 56, 405, 51);
		scorePanel.add(wordLabel);
		
		JPanel timerPanel = new JPanel() {
			public void paintComponent(Graphics g) {
                g.drawImage(timerBackground, 0, 0, this.getWidth(), this.getHeight(), null);
                setOpaque(false);
                super.paintComponent(g);
            }
		};
		timerPanel.setBounds(545, 78, 120, 120);
		timerPanel.setLayout(null);
		
		JLabel scoreLabel = new JLabel("30");
		scoreLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 20));
		scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
		scoreLabel.setForeground(new Color(251, 255, 250));
		scoreLabel.setBounds(6, 35, 108, 50);
		timerPanel.add(scoreLabel);
		
		contentPanel.add(timerPanel);
		
		//이미지 버튼 추가
		happyEmo = imageSetSize(happyEmo,30,30);
		JButton happyemoButton = new JButton();
		happyemoButton.setIcon(happyEmo);
		happyemoButton.setBounds(6, 406, 35, 35);
		contentPanel.add(happyemoButton);
		
		loveEmo = imageSetSize(loveEmo,30,30);
		JButton loveButton = new JButton();
		loveButton.setIcon(loveEmo);
		loveButton.setBounds(42, 406, 35, 35);
		contentPanel.add(loveButton);
		
		supriseEmo = imageSetSize(supriseEmo,30,30);
		JButton supriseButton = new JButton();
		supriseButton.setIcon(supriseEmo);
		supriseButton.setBounds(78, 406, 35, 35);
		contentPanel.add(supriseButton);
		
		sleepEmo = imageSetSize(sleepEmo,30,30);
		JButton sleepeButton = new JButton();
		sleepeButton.setIcon(sleepEmo);
		sleepeButton.setBounds(114, 406, 35, 35);
		contentPanel.add(sleepeButton);
		
		AngryEmo = imageSetSize(AngryEmo,30,30);
		JButton angryButton = new JButton();
		angryButton.setIcon(AngryEmo);
		angryButton.setBounds(150, 406, 35, 35);
		contentPanel.add(angryButton);
	}
	
	ImageIcon imageSetSize(ImageIcon icon, int i, int j) { // image Size Setting
		Image ximg = icon.getImage();  //ImageIcon을 Image로 변환.
		Image yimg = ximg.getScaledInstance(i, j, java.awt.Image.SCALE_SMOOTH);
		ImageIcon xyimg = new ImageIcon(yimg); 
		return xyimg;
	}
}