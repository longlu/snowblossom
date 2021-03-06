package snowblossom.iceleaf;

import java.awt.Color;
import java.awt.Font;
import java.io.InputStream;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import snowblossom.client.StubHolder;
import snowblossom.iceleaf.components.*;
import snowblossom.lib.Globals;
import snowblossom.lib.NetworkParams;
import snowblossom.lib.NetworkParamsProd;

public class IceLeaf
{
  public static void main(String args[]) throws Exception
  {
    Globals.addCryptoProvider();

    new IceLeaf(new NetworkParamsProd(), null);

  }

  protected Preferences ice_leaf_prefs;
  protected NodePanel node_panel;
  protected NodeSelectionPanel node_select_panel;
  protected WalletPanel wallet_panel;
  protected MakeWalletPanel make_wallet_panel;
  protected SendPanel send_panel;
  protected AddressPanel address_panel;
  protected NetworkParams params;
  protected SettingsPanel settings_panel;
  protected ReceivePanel receive_panel;
  protected IceLeaf ice_leaf;

  public Preferences getPrefs() { return ice_leaf_prefs;}
  public NetworkParams getParams() { return params; }
  public StubHolder getStubHolder(){return node_select_panel.getStubHolder();}
  public WalletPanel getWalletPanel(){return wallet_panel;}
  public SendPanel getSendPanel(){return send_panel;}

  public Color getBGColor(){return new Color(204,204,255);}
  public Color getTextAreaBGColor(){return new Color(220,220,220);}

  private Font fixed_font;
  private Font bold_fixed_font;
  private Font var_font;

  public Font getFixedFont(){return fixed_font;}
  public Font getBoldFixedFont(){return bold_fixed_font;}
  public Font getVariableFont(){return var_font;}


  public String getResourceBasePath(){return ""; }
  

  public IceLeaf(NetworkParams params, Preferences prefs)
    throws Exception
  {
    this.params = params;
    this.ice_leaf_prefs = prefs;
    this.ice_leaf = this;
    if (ice_leaf_prefs == null)
    {
      ice_leaf_prefs = Preferences.userNodeForPackage(this.getClass());
    }
    
    SwingUtilities.invokeAndWait(new EnvSetup());

    node_panel = new NodePanel(this);
    node_select_panel = new NodeSelectionPanel(this);
    wallet_panel = new WalletPanel(this);
    make_wallet_panel = new MakeWalletPanel(this);
    send_panel = new SendPanel(this);
    address_panel = new AddressPanel(this);
    receive_panel = new ReceivePanel(this);
    settings_panel = new SettingsPanel(this);

    SwingUtilities.invokeLater(new WindowSetup());

  }



  public class EnvSetup implements Runnable
  {
    public void run()
    {
      try
      {
        fixed_font = Font.createFont(Font.TRUETYPE_FONT, 
          IceLeaf.class.getResourceAsStream( getResourceBasePath() + "/iceleaf-ui/resources/font/Hack-Regular.ttf"));
        bold_fixed_font = Font.createFont(Font.TRUETYPE_FONT, 
          IceLeaf.class.getResourceAsStream( getResourceBasePath() +"/iceleaf-ui/resources/font/Hack-Bold.ttf"));
        var_font = new Font("Verdana", 0, 12);

        //IceLeaf.setUIFont(new Font("Verdana", 0, 12));
        //UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }



  public String getTitle()
  {
    String title = "SnowBlossom - IceLeaf " + Globals.VERSION;
    return title;

  }

  public class WindowSetup implements Runnable
  {
    public void run()
    {

      JFrame f=new JFrame();
      f.setVisible(true);
      f.setDefaultCloseOperation( f.EXIT_ON_CLOSE);
      f.setBackground(getBGColor());

      try
      {
        InputStream is = IceLeaf.class.getResourceAsStream(getResourceBasePath() +"/iceleaf-ui/resources/flower-with-ink-256.png");
        f.setIconImage(ImageIO.read(is));

      }
      catch(Exception e)
      {
        e.printStackTrace();
      }


      String title = getTitle();
      if (!params.getNetworkName().equals("snowblossom"))
      {
        title = title + " - " + params.getNetworkName();
      }
      f.setTitle(title);
      f.setSize(950, 600);

      JTabbedPane tab_pane = new JTabbedPane();

      f.setContentPane(tab_pane);

      // should be first to initialize settings to default
      settings_panel.setup();

      node_panel.setup();
      node_select_panel.setup();
      wallet_panel.setup();
      make_wallet_panel.setup();
      send_panel.setup();
      address_panel.setup();
      receive_panel.setup();

      tab_pane.add("Wallets", wallet_panel.getPanel());
      tab_pane.add("Send", send_panel.getPanel());
      tab_pane.add("Receive", receive_panel.getPanel());
      tab_pane.add("Addresses", address_panel.getPanel());
      tab_pane.add("Make Wallet", make_wallet_panel.getPanel());
      tab_pane.add("Node Selection", node_select_panel.getPanel());
      tab_pane.add("Node", node_panel.getPanel());
      tab_pane.add("Settings", settings_panel.getPanel());

      setupMorePanels(tab_pane, f);

      UIUtil.applyLook(f, ice_leaf);
      
    }

  }

  public void setupMorePanels( JTabbedPane tab_pane, JFrame frame)
  {

  }

}
