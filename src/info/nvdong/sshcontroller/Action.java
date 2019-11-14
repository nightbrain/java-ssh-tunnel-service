package info.nvdong.sshcontroller;

public class Action {
  private String action, data;

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String toString() {
    return this.action + ": " + this.data;
  }
}
