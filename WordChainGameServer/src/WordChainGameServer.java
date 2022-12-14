import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class WordChainGameServer extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	JTextArea textArea;
	private JTextField txtPortNumber;

	private ServerSocket socket; // 서버소켓
	private Socket client_socket; // accept() 에서 생성된 client 소켓
	private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의
	
	private Words words = null;
	private Vector UserVec = new Vector(); // 연결된 사용자를 저장할 벡터
	private Vector<String> WaitUserVec = new Vector<String>(); // 접속자 리스트를 저장하는 벡터
	private Vector<Room> RoomVec = new Vector<Room>(); // 모든 방 리스트를 담는 벡터
	private Vector RoomUserListVec = new Vector();
	private HashMap<Integer, Integer> RoomTurnList = new HashMap<Integer,Integer>();
	
	private Room myRoom; // 유저가 입장한 대화방
	private int totalRoomCount = 0;
	private int roomUserListSeq = 0;
	
	private final String fileName = "wordList.txt";
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WordChainGameServer frame = new WordChainGameServer();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public WordChainGameServer() {
		this.words = new Words(fileName);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 338, 440);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 10, 300, 298);
		contentPane.add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);

		JLabel lblNewLabel = new JLabel("Port Number");
		lblNewLabel.setBounds(13, 318, 87, 26);
		contentPane.add(lblNewLabel);

		txtPortNumber = new JTextField();
		txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
		txtPortNumber.setText("30000");
		txtPortNumber.setBounds(112, 318, 199, 26);
		contentPane.add(txtPortNumber);
		txtPortNumber.setColumns(10);

		JButton btnServerStart = new JButton("Server Start");
		btnServerStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
				} catch (NumberFormatException | IOException e1) {
					e1.printStackTrace();
				}
				AppendText("Chat Server Running..");
				btnServerStart.setText("Chat Server Running..");
				btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
				txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
				AcceptServer accept_server = new AcceptServer();
				accept_server.start();
			}
		});
		btnServerStart.setBounds(12, 356, 300, 35);
		contentPane.add(btnServerStart);
	}

	// 새로운 참가자 accept() 하고 user thread를 새로 생성한다.
	class AcceptServer extends Thread {
		@SuppressWarnings("unchecked")
		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					AppendText("Waiting new clients ...");
					client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
					AppendText("새로운 참가자 from " + client_socket);
					// User 당 하나씩 Thread 생성
					UserService new_user = new UserService(client_socket);
					UserVec.add(new_user); // 새로운 참가자 배열에 추가
					new_user.start(); // 만든 객체의 스레드 실행
					AppendText("현재 참가자 수 " + UserVec.size());
				} catch (IOException e) {
					AppendText("accept() error");
					// System.exit(0);
				}
			}
		}
	}

	public void AppendText(String str) {
		// textArea.append("사용자로부터 들어온 메세지 : " + str+"\n");
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	public void AppendObject(ChatMsg msg) {
		// textArea.append("사용자로부터 들어온 object : " + str+"\n");
		textArea.append("code = " + msg.code + "\n");
		textArea.append("id = " + msg.UserName + "\n");
		textArea.append("data = " + msg.data + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	// User 당 생성되는 Thread
	// Read One 에서 대기 -> Write All
	class UserService extends Thread {
		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		private Socket client_socket;
		private Vector user_vc;
		public String UserName = "";
		public String UserStatus;
		public String UserMode; // 게임참여 or 관전모드 를 받기 위한 변수 
		public int UserScore = 0;
		public boolean isTurn = false;
		public int roomNumber = 0;
		//Timer timer;

		public UserService(Socket client_socket) {
			// 매개변수로 넘어온 자료 저장
			this.client_socket = client_socket;
			this.user_vc = UserVec;
			try {
				oos = new ObjectOutputStream(client_socket.getOutputStream());
				oos.flush();
				ois = new ObjectInputStream(client_socket.getInputStream());
			} catch (Exception e) {
				AppendText("userService error");
			}
		}

		
		public void Login() {
			AppendText("새로운 참가자 " + UserName + " 입장.");
			String msg = "[" + UserName + "]님이 입장 하였습니다.\n";
			EnterAlarmAll(); // 새로 접속한 유저 리스트 알림
			CreateRoomAlarmAll();
		}

		public void Logout() {
			String msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
			UserVec.removeElement(this); // Logout한 현재 객체를 벡터에서 지운다
			WaitUserVec.remove(UserName);
			AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + UserVec.size());
			EnterAlarmAll();
		}
		
		public String RoomInfo(Room myRoom) {
			String tmp = "";
			tmp += myRoom.roomNumber + "#";
			tmp += myRoom.roomTitle + "#";
			tmp += myRoom.roomManager + "#";
			tmp += myRoom.roomCount + "#";
			tmp += myRoom.roundTime + "#";
			tmp += myRoom.roundCount + "#";
			tmp += UserScore + "#";
			tmp += myRoom.start + "#";
			tmp += myRoom.userList +"|";
			tmp += myRoom.userStatusList;
			return tmp;
		}
		
		
		// 클라이언트에게 턴 알려주기
		public void AlarmToTurn(int roomNumber, UserService u, String type, String answer) {
			String msg = roomNumber + "번째 방의 턴 :" +u.getName();
			AppendText(msg);
			u.setTurn(true);// turn을 변경 
			System.out.println("현재 턴 사용자 : "+u.UserName+", 턴 플래그 "+u.isTurn);
			for(int i=0;i<RoomUserListVec.size();i++) {
				UserService us = (UserService) RoomUserListVec.elementAt(i);
				us.AlarmToTurnOne(roomNumber, u.UserName);
				us.sendWordAll(type, answer); //제시어 호출 메소드
			}
		}
		
		public void AlarmToTurnOne(int roomNumber,String userName) {
			try {
				ChatMsg obcm = new ChatMsg("SEVER", "306", userName);
				obcm.SetRoomNumber(roomNumber);
				oos.writeObject(obcm);
				AppendText(roomNumber+"번째 방 턴: "+userName);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		//차례 변경하기
		public void setTurn(boolean isTurn) {
			this.isTurn = isTurn;
		}

		//방번호로 방 객체 찾아서 사용자 리스트 가져오기
		public Vector<UserService> getRoomUserList(int roomNumber) {
			String userList[] = null;
			Vector<UserService> user = new Vector();
			for(int i=0;i<RoomVec.size();i++) {
				Room r = RoomVec.elementAt(i);
				if(r.roomNumber == roomNumber) {
					userList = (String[])r.userList.split("@");
					break;
				}
			}
			if(userList.length > 0) {
				for(int i=0;i<userList.length;i++) {
					for(int j=0;j<user_vc.size();j++) {
						UserService u = (UserService)user_vc.elementAt(j);
						if(userList[i].equals(u.UserName)) {
							user.add(u); 
							break;
						}
					}
				}
			}
			return user;
			
		}
		
		// 게임 방 입장 알림
		public void GameRoomEnterAlarm(Room myRoom) {
			// 방번호#방제목#방장#들어온 인원수#라운드시간#라운드수#개인점수#게임시작상태#user1@user2@|game@watch
			String tmp = RoomInfo(myRoom);
			String [] users = myRoom.userList.split("@");
			
			//room.userList에 마지막인자 == 최근입장한 사람 -> 301 호
			for(int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i); // 가장 최근에 들어온 사람
				for(int j = 0; j < users.length; j++) {
					if(user.UserName.equals(users[users.length - 1])) {
						user.GameRoomEnterAlarmOne("create", tmp);
						WriteOne(UserName + "님 환영합니다.\n");
						break;
					} else if(user.UserName.equals(users[j])){
						EnterAlarmAll();
						user.GameRoomEnterAlarmOne("noti", tmp);
						break;
					}
				}
			}
		}
		
		// 일반유저가 방을 나갈때 방안에 있는 유저들에게 방송
		public void RoomExit(Room myRoom, String deleteUser) {
			// 방번호#방제목#방장#들어온 인원수#라운드시간#라운드수#개인점수#게임시작상태#user1@user2@
			String tmp = RoomInfo(myRoom);
			if(myRoom.userList.contains("@")) {
				String userlist = tmp.split("#")[8];
				String [] users = userlist.split("@");
				
				for(int i = 0; i < user_vc.size(); i++) {
					UserService user = (UserService) user_vc.elementAt(i);
					for(int j = 0; j < users.length; j++) {
						if(user.UserName.equals(users[j])) {
							user.GameRoomExitAlarmOne(userlist, deleteUser);
						}
					}
				}
			}
		}
		
		// 방의 상태를 알려줌
		public void CantGameEnterAlarm(String UserName, String type, String roomInfo) {
			for(int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if(user.UserName.equals(UserName)) {
					user.CantGameEnterAlarmOne(type,roomInfo);
					break;
				}
			}
		}
		
		// 방 만드는 방송
		public void CreateRoomAlarmAll() {
			String tmp = "";
			for(int i = 0; i < RoomVec.size(); i++) {
				tmp += RoomVec.get(i).roomTitle + ",";
			}
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.CreateRoomAlarmOne(tmp);
			}
		}
		
		public void ScoreAlarm(Room myRoom, String answerUsername, String type) {
			// 방번호#방제목#방장#들어온 인원수#라운드시간#라운드수#개인점수#게임시작상태#user1@user2@
			String tmp = RoomInfo(myRoom);
			if(myRoom.userList.contains("@")) {
				String userlist = tmp.split("#")[8];
				String [] users = userlist.split("@");
							
				for(int i = 0; i < user_vc.size(); i++) {
					UserService user = (UserService) user_vc.elementAt(i);
					for(int j = 0; j < users.length; j++) {
						if(user.UserName.equals(users[j])) {
							user.ScoreAlarmOne(answerUsername, type);
						}
					}
				}
			}			
		}
		
		// 모든 User들에게 방송. 각각의 UserService Thread의 EnterAlarmOne() 을 호출한다.
		public void EnterAlarmAll() {
			String tmp = "";
			for(int i = 0; i < WaitUserVec.size(); i++) {
				tmp += WaitUserVec.get(i) + ",";
			}
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.EnterAlarmOne(tmp);
			}
		}
		
		// 모든 User들에게 방송. 각각의 UserService Thread의 WriteOne() 을 호출한다.
		public void WriteAll(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteOne(str);
			}
		}
		// 모든 User들에게 Object를 방송. 채팅 message와 image object를 보낼 수 있다
		public void WriteAllObject(Object ob, Room myRoom) {
			String tmp = RoomInfo(myRoom);
			if(myRoom.userList.contains("@")) {
				String userlist = tmp.split("#")[8];
				String [] users = userlist.split("@");
							
				for(int i = 0; i < user_vc.size(); i++) {
					UserService user = (UserService) user_vc.elementAt(i);
					for(int j = 0; j < users.length; j++) {
						if(user.UserName.equals(users[j])) {
							user.WriteOneObject(ob);
						}
					}
				}
			}
		}

		// 나를 제외한 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteOthers(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user != this && user.UserStatus == "O")
					user.WriteOne(str);
			}
		}

		// Windows 처럼 message 제외한 나머지 부분은 NULL 로 만들기 위한 함수
		public byte[] MakePacket(String msg) {
			byte[] packet = new byte[BUF_LEN];
			byte[] bb = null;
			int i;
			for (i = 0; i < BUF_LEN; i++)
				packet[i] = 0;
			try {
				bb = msg.getBytes("euc-kr");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			for (i = 0; i < bb.length; i++)
				packet[i] = bb[i];
			return packet;
		}
		
		public void ScoreAlarmOne(String answerUsername, String type) {
			try {
				if (type.equals("correct")) {
					ChatMsg r_ob = new ChatMsg(UserName, "203", "correct");
					r_ob.answerUsername = answerUsername;
					oos.writeObject(r_ob);
				}
				else if (type.equals("wrong")) {
					ChatMsg r_ob = new ChatMsg(UserName, "203", "wrong");
					r_ob.answerUsername = answerUsername;
					oos.writeObject(r_ob);
				}
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		public void CantGameEnterAlarmOne(String type,String roomInfo) {
			try {
				if (type.equals("Full")) {
					ChatMsg r_ob = new ChatMsg(UserName, "308", "Full$"+roomInfo);
					oos.writeObject(r_ob);
				} else if (type.equals("Started")) {
					ChatMsg r_ob = new ChatMsg(UserName, "308", "Started$"+roomInfo);
					oos.writeObject(r_ob);
				}
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		public void GameRoomExitAlarmOne(String userlist, String deleteUser) {
			try {
				ChatMsg r_ob = new ChatMsg(UserName, "400", userlist);
				r_ob.SetDeleteUser(deleteUser);
				oos.writeObject(r_ob);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		public void CreateRoomAlarmOne(String title) {
			try {
				ChatMsg r_ob = new ChatMsg(UserName, "302", title);
				r_ob.SetRoomTitle(title);
				oos.writeObject(r_ob);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		public void GameRoomEnterAlarmOne(String type, String msg) {
			try {
				if(type.equals("create")) {
					ChatMsg r_ob = new ChatMsg(UserName, "301", msg);
					oos.writeObject(r_ob);
				} else if (type.equals("noti")){
					ChatMsg r_ob = new ChatMsg(UserName, "307", msg);
					oos.writeObject(r_ob);
				}
				
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		public void GameExitAlarmOne(String type, String userName) {
			try {
				if(type.equals("One Exit")) {
					ChatMsg r_ob = new ChatMsg(UserName, "400", userName);
					oos.writeObject(r_ob);
				} else {
					ChatMsg r_ob = new ChatMsg(UserName, "402", userName);
					oos.writeObject(r_ob);
				}
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		// 현재 접속자 리스트 전송
		public void EnterAlarmOne(String msg) {
			try {
				ChatMsg obcm = new ChatMsg("Enter", "100", msg);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}

		// UserService Thread가 담당하는 Client 에게 1:1 전송
		public void WriteOne(String msg) {
			try {
				ChatMsg obcm = new ChatMsg("SERVER", "200", msg);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}

		// 귓속말 전송
		public void WritePrivate(String msg) {
			try {
				ChatMsg obcm = new ChatMsg("귓속말", "201", msg);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		public void WriteOneObject(Object ob) {
			try {
			    oos.writeObject(ob);
			} 
			catch (IOException e) {
				AppendText("oos.writeObject(ob) error");		
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;				
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout();
			}
		}
		
		//제시어 전송
		public void sendWordAll(String type, String answer) {
			String word = null;
			if (type.equals("first"))
				word = words.getRandomWord();
			else if (type.equals("not first"))
				word = answer;
				
			for(int i=0;i<RoomUserListVec.size();i++) {
				UserService u = (UserService) RoomUserListVec.elementAt(i);
				u.sendWord(word);
			}
		}
		
		//결과 전송 
		public void sendResult(String msg) {
			try {
				ChatMsg obcm = new ChatMsg("SERVER", "401", msg);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		public void sendWord(String word) {
			try {
				ChatMsg obcm = new ChatMsg(UserName, "304", word);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		//시간전송
		public void sendTimeAll() {
			for(int i=0;i<RoomUserListVec.size();i++) {
				UserService u = (UserService) RoomUserListVec.elementAt(i);
				u.sendTimeInfo();
			}
		}
		
		public void sendTimeInfo() {
			try {
				ChatMsg obcm = new ChatMsg(UserName, "309", "timerStart");
				obcm.SetRoomNumber(roomNumber);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		//게임 시작 버튼
		public void gameStartAll() {
			for(int i=0;i<RoomUserListVec.size();i++) {
				UserService u = (UserService) RoomUserListVec.elementAt(i);
				u.gameStart();
			}
		}
				
		//게임 시작 버튼
		public void gameStart() {
			try {
				ChatMsg obcm = new ChatMsg("SERVER", "303", "gameStart");
				obcm.SetRoomNumber(roomNumber);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		//이모티콘 전송
		public void AlarmEmotion(String UserName, String type) {
			String msg = UserName+"&"+type; //ex. User1&happy
			System.out.println(msg);
			try {
				ChatMsg obcm = new ChatMsg("SERVER", "202", msg);
				obcm.SetRoomNumber(roomNumber);
				oos.writeObject(obcm);
			} catch (IOException e) {
				AppendText("dos.writeObject() error");
				try {
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Logout(); // 에러가난 현재 객체를 벡터에서 지운다
			}
		}
		
		//게임방의 1등 구하는 함수
		public String getFirstWinUser(int roomNumber) {
			
			RoomUserListVec = getRoomUserList(roomNumber);
			
			String msg = "";
		
			int[] score = new int[RoomUserListVec.size()];
			for(int i=0;i<score.length;i++) {
				UserService u = (UserService) RoomUserListVec.elementAt(i);
				if(u.UserMode.equals("game")) {
					score[i] = u.UserScore;
				}
			}
			
			int[] rank = new int[RoomUserListVec.size()];
			Arrays.fill(rank, 1); //배열 원소를 1로 초기화 
			
			for(int i=0;i<score.length;i++) {
				for(int j=0;j<score.length;j++) {
					if(score[j] > score[i]) {
						rank[i] = rank[i]+1;
					}
				}
			}

			int rankFlag = 0; //무승부를 체크 하는 값 
			for(int i=0;i<RoomUserListVec.size();i++) {
				UserService u1 = (UserService) RoomUserListVec.elementAt(i);
				if(u1.UserMode.equals("game")) {
					if(rank[i] == 1) {
						rankFlag++;
						if(rankFlag == 1) {
							msg =u1.UserScore +"&"+u1.UserName;
						}else {
							msg+="@"+u1.UserName;
						}
					}
				}
			}
			
			return msg;
		}

		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					Object obcm = null;
					String msg = null;
					ChatMsg cm = null;
					if (socket == null)
						break;
					try {
						obcm = ois.readObject();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return;
					}
					if (obcm == null)
						break;
					if (obcm instanceof ChatMsg) {
						cm = (ChatMsg) obcm;
						AppendObject(cm);
					} 
					else
						continue;
					
					if (cm.code.matches("100")) {
						UserName = cm.UserName;
						WaitUserVec.add(cm.UserName); // 새로 들어온 유저를 접속자 리스트에 넣음
						UserStatus = "O"; // Online 상태
						EnterAlarmAll();
						Login();

					} else if (cm.code.matches("200")) {
						msg = String.format("[%s] %s", cm.UserName, cm.data);
						AppendText(msg); // server 화면에 출력
						String[] args = msg.split(" "); // 단어들을 분리한다.
						if (args.length == 1) { // Enter key 만 들어온 경우 Wakeup 처리만 한다.
							UserStatus = "O";
						} else if (args[1].matches("/exit")) {
							Logout();
							break;
						} else if (args[1].matches("/list")) {
							WriteOne("User list\n");
							WriteOne("Name\tStatus\n");
							WriteOne("-----------------------------\n");
							for (int i = 0; i < user_vc.size(); i++) {
								UserService user = (UserService) user_vc.elementAt(i);
								WriteOne(user.UserName + "\t" + user.UserStatus + "\n");
							}
							WriteOne("-----------------------------\n");
						} else if (args[1].matches("/sleep")) {
							UserStatus = "S";
						} else if (args[1].matches("/wakeup")) {
							UserStatus = "O";
						}else { // 일반 채팅 메시지
							for (int i=0; i<RoomVec.size(); i++) {
								Room room = RoomVec.get(i);
								
								if (room.roomNumber == cm.roomNumber) {
									UserStatus = "O";
									WriteAllObject(cm, room);
								}
							}
						}
					}else if(cm.code.matches("201")) { //귓속말 
						msg = String.format("[%s] %s", cm.UserName, cm.data);
						AppendText(msg); // server 화면에 출력
						String[] args = msg.split(" "); // 단어들을 분리한다.
						
						for (int i = 0; i < user_vc.size(); i++) {
							UserService user = (UserService) user_vc.elementAt(i);
							if (user.UserName.matches(args[2]) && user.UserStatus.matches("O")) {
								String msg2 = "";
								for (int j = 3; j < args.length; j++) {// 실제 message 부분
									msg2 += args[j];
									if (j < args.length - 1)
										msg2 += " ";
								}
								// /to 빼고.. [귓속말] [user1] Hello user2..
								user.WritePrivate(args[0] + " " + msg2 + "\n");
								break;
							}
						}
					}else if(cm.code.matches("202")) { //이모티콘 전달받음 
						msg = String.format("[%s] %s", cm.UserName, cm.data);
						AppendText(msg);
						String name = cm.UserName;
						String type = cm.data;
						roomNumber = cm.roomNumber;
						
						RoomUserListVec = getRoomUserList(roomNumber);
						
						for(int i=0;i<RoomUserListVec.size();i++) {
							UserService u = (UserService) RoomUserListVec.elementAt(i);
							u.AlarmEmotion(name,type);
						}
					}else if(cm.code.matches("203")) { // 답 입력 
						msg = String.format("[%s] %s", cm.UserName, cm.data);
						AppendText(msg);
						
						for(int i = 0; i < RoomVec.size(); i++) {
							Room room = RoomVec.get(i);
							if(room.roomNumber == cm.roomNumber) { // 일치하는 방 찾기
								myRoom = room;  // 들어간 방 설정
								break;
							}
						}
						
						if ((cm.currentQ.charAt(cm.currentQ.length() - 1) == cm.data.charAt(0)) || cm.data.equals("over")) {
							boolean compareWord = words.compareWord(cm.data);
							if (compareWord) { // 정답일 때
								RoomUserListVec = getRoomUserList(cm.roomNumber);
								
								int user_seq = RoomTurnList.get(cm.roomNumber);
								user_seq++;
								
								if(user_seq == RoomUserListVec.size()) {
									user_seq = 0; //초기화 
								}
								
								//관전모드인 사람 턴넘기기 
								UserService u1 = (UserService) RoomUserListVec.elementAt(user_seq);
								if(u1.UserMode.equals("watch")) {
									user_seq++;
									if(user_seq == RoomUserListVec.size()) {
										user_seq = 0; //초기화 
									}
								}

								RoomTurnList.put(cm.roomNumber, user_seq); //방마다 턴을 저장함 
							
								UserService u = (UserService) RoomUserListVec.elementAt(user_seq);
								
								AlarmToTurn(cm.roomNumber, u, "not first", cm.data); // 턴 전송
								sendTimeAll();
								ScoreAlarm(myRoom, cm.UserName, "correct"); // 정답을 맞출 때마다 점수 오르는 방송
							}
							else { // wordList에 없는 단어를 입력했을 때
								RoomUserListVec = getRoomUserList(cm.roomNumber);
									
								int user_seq = RoomTurnList.get(cm.roomNumber);
								user_seq++;
								
								if(user_seq == RoomUserListVec.size()) {
									user_seq = 0; //초기화 
								}
								
								//관전모드인 사람 턴넘기기 
								UserService u1 = (UserService) RoomUserListVec.elementAt(user_seq);
								if(u1.UserMode.equals("watch")) {
									user_seq++;
									if(user_seq == RoomUserListVec.size()) {
										user_seq = 0; //초기화 
									}
								}
				
								RoomTurnList.put(cm.roomNumber, user_seq); //방마다 턴을 저장함 
									
								UserService u = (UserService) RoomUserListVec.elementAt(user_seq);
								
								AlarmToTurn(cm.roomNumber, u, "not first", cm.currentQ); // 턴 전송
								sendTimeAll();
								ScoreAlarm(myRoom, cm.UserName, "wrong");
								
							}
						}

					}else if(cm.code.matches("301")) { // 방 입장
						UserMode = "game";
						for(int i = 0; i < RoomVec.size(); i++) {
							Room room = RoomVec.get(i);
							
							if(room.roomNumber == cm.roomNumber) { // 일치하는 방 찾기
								if (cm.onStart == true) { // 해당 방 방장이 게임 시작 버튼을 눌렀을 때 입장 불가
									room.start = true;
									break;
								}
								if (room.roomCount < 5 && room.start == false) { // 최대 5명만 입장 가능
									myRoom = room;  // 들어간 방 설정
									myRoom.roomCount++; // 인원수 증가
									myRoom.setUserList(UserName); // 게임방에 접속한 유저를 추가
									myRoom.setUserStatusList("game"); // 게임방에 접속한 유저의 상태를 추가
									System.out.println("이 방에 총 " + myRoom.roomCount + "명 들어와 있음, 총 들어잇는 사람" + myRoom.userList);
									
									// 유저를 게임방으로 이동시키기
									WaitUserVec.remove(UserName); // 대기실 벡터에서 해당 사용자 빼기
									GameRoomEnterAlarm(myRoom);
								}
								else { 
									if (room.start == true) {
										CantGameEnterAlarm(UserName, "Started","");
									}else {
										UserMode = "watch";
										myRoom = room;
										myRoom.setUserList(UserName);
										myRoom.setUserStatusList("watch"); // 게임방에 접속한 유저의 상태를 추가
										
										String tmp = RoomInfo(myRoom);
									
										WaitUserVec.remove(UserName); // 대기실 벡터에서 해당 사용자 빼기	
										
										CantGameEnterAlarm(UserName, "Full",tmp);
									}
										
								}
								System.out.println("방 참가자 :"+myRoom.userList);
								break;
							}
						}
						
					} else if(cm.code.matches("302")) { // 방 만들기 처리
						UserMode = "game";
						myRoom = new Room(); // 새로운 방 생성
						myRoom.roomNumber = totalRoomCount++; // 1번 방
						myRoom.roomTitle = cm.roomTitle; // 방 제목 저장
						myRoom.roomManager = UserName; // 방장 설정
						myRoom.roomCount += 1; // 방 인원 수 증가
						myRoom.setUserList(UserName); // 게임방에 접속한 유저를 추가(방마다 유저가 몇명있는지 관리)
						myRoom.setUserStatusList("game");
						
						RoomVec.add(myRoom); // 현재 만든 방을 모든방리스트 벡터에 추가
						WaitUserVec.remove(UserName); // 대기실 벡터에서 해당 사용자 빼기
						EnterAlarmAll();
						CreateRoomAlarmAll(); // 대기실에 있는 유저들에게 방정보 출력
						GameRoomEnterAlarm(myRoom);
						
					} else if(cm.code.matches("303")) { //게임시작 
						AppendText(msg);
						roomNumber = (int)cm.roomNumber;
						RoomUserListVec = getRoomUserList(roomNumber);
						
						int user_seq = 0; //게임시작했을때 턴은 0으로 
						RoomTurnList.put(roomNumber, user_seq); //방마다 턴을 저장함 
						
						UserService u = (UserService) RoomUserListVec.elementAt(user_seq);
						AlarmToTurn(roomNumber, u, "first", cm.data); 
						sendTimeAll();
						gameStartAll();
					} else if(cm.code.matches("305")) { //사용자 점수 관리 
						String name = cm.data.split("&")[0];
						int score = Integer.parseInt(cm.data.split("&")[1]);
						
						roomNumber = (int)cm.roomNumber;
						RoomUserListVec = getRoomUserList(roomNumber);
						
						for(int i=0;i<RoomUserListVec.size();i++) {
							UserService u = (UserService)RoomUserListVec.elementAt(i);
							if(u.UserName.equals(name)) {
								u.UserScore = score;
							}
						}
					} else if (cm.code.matches("400")) { // 게임방 퇴장
						for(int i=0;i<RoomVec.size();i++) {
							Room room = RoomVec.get(i);
							if(cm.roomNumber == room.roomNumber) {
								// 팀원의 이름과 상태를 빼줌
								String[] userlist = room.userList.split("@");
								String[] userStatus = room.userStatusList.split("@");
								String newUserList = "";
								String newStatusList = "";
								
								boolean watchFlag = false;
								
								for(int j =0;j<userlist.length;j++) {
									if(cm.UserName.equals(userlist[j])){
										if(userStatus[j].equals("watch")) {
											watchFlag = true;
										}
										continue;
									}else {
										newUserList += userlist[j]+"@";
										newStatusList += userStatus[j]+"@";
									}
									
								}
								if(watchFlag == false) {
									room.roomCount--; // 방의 인원수 한명 줄임
								}
								
								room.userList = newUserList;
								room.userStatusList = newStatusList;
								AppendText(room.userList);
								AppendText(room.userStatusList);
								AppendText(String.valueOf(room.roomCount));
								// room.userList = room.userList.replace(cm.UserName + "@", ""); // 해당 유저를 방 리스트에서 지움
								
								RoomExit(room, cm.UserName);
								if (room.roomCount == 0) { // 마지막 남은 한 사람이 나갈 때 RoomVec에서도 이 방을 지움
									RoomVec.remove(room);
								}
								break;
							}
						}
						
					} else if (cm.code.matches("401")) { // 게임 종료
						// 게임 종료 시 1위만 보여줌 
						// 데이터에 이름@점수 데이터만 전송

						roomNumber =cm.roomNumber;
						RoomUserListVec = getRoomUserList(roomNumber);
						
						msg = getFirstWinUser(roomNumber); //사용자명&점수

						sendResult(msg);
						
						break;
					}
				} catch (IOException e) {
					AppendText("ois.readObject() error");
					try {
						ois.close();
						oos.close();
						client_socket.close();
						Logout(); // 에러가난 현재 객체를 벡터에서 지운다
						break;
					} catch (Exception ee) {
						break;
					} // catch문 끝
				} // 바깥 catch문끝
			} // while
		} // run
	}

	// wordList.txt 파일을 읽고 벡터에 저장하고 벡터로부터 랜덤하게 단어를 추출하는 클래스
	class Words {
		private Vector<String> wordVector = new Vector<String>(20000);

		public Words(String fileName) {
			try {
				Scanner scanner = new Scanner(new FileReader(fileName));
				while (scanner.hasNext()) { // 파일 끝까지 읽음
					String words = scanner.nextLine();
					wordVector.add(words.trim());
				}
			} catch (FileNotFoundException e) {
				System.out.println("file is not found");
				System.exit(0);
			}
		}

		public String getRandomWord() {
			int index = (int) (Math.random() * wordVector.size());
			return wordVector.get(index);
		}
		
		public boolean compareWord(String input) { // 사용자가 입력한 단어가 wordList에 있는지 확인
			for(int i=0; i<wordVector.size(); i++) {
				if(input.equals(wordVector.get(i))) {
					return true;
				}
			}
			return false;
		}
	}
	
}