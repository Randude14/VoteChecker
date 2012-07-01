package com.randude14.votechecker;

public class Voter implements Comparable<Voter> {
	private String player;
	private int votes;
	
	public Voter(String player) {
		this.player = VoteChecker.matchPlayer(player);
		votes = 1;
	}
	
	public String getName() {
		return player;
	}
	
	public int getVotes() {
		return votes;
	}
	
	public void inc() {
		votes++;
	}

	public boolean equals(String name) {
		if(player.equalsIgnoreCase(name))
			return true;
		String nameComp = name;
		String playerComp = player;
		for(char c : VoteChecker.specialChars) {
			nameComp = nameComp.replace(Character.toString(c), "");
			playerComp = playerComp.replace(Character.toString(c), "");
		}
		return nameComp.equalsIgnoreCase(playerComp);
	}
	
	public int compareTo(Voter voter) {
		int comp = voter.votes - votes;
		if(comp == 0)
			return player.compareToIgnoreCase(voter.player);
		return comp;
	}
	
	public String toString() {
		return player;
	}

}
