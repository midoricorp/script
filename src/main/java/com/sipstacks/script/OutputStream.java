package com.sipstacks.script;

public class OutputStream {
	private StringBuffer txt;
	private StringBuffer html;

	public OutputStream() {
		txt = new StringBuffer();
		html = new StringBuffer();
	}

	public OutputStream append(OutputStream os) {
		txt.append(os.txt);
		html.append(os.html);
		return this;
	}

	public OutputStream appendText(String txt) {
		this.txt.append(txt);
		return this;
	}

	public OutputStream trimText() {
		if (this.txt.charAt(this.txt.length()-1) == '\n') {
			this.txt.setLength(this.txt.length()-1);
		}
		return this;
	}

	public OutputStream appendHtml(String html) {
		this.html.append(html);
		return this;
	}

	public String getText() {
		return txt.toString();
	}

	public String getHtml() {
		return html.toString();
	}

	public String toString() {
		return getText();
	}

}
