package snowblossom.shackleton;

import duckutil.Config;
import duckutil.ConfigFile;
import io.grpc.ManagedChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import snowblossom.client.GetUTXOUtil;
import snowblossom.client.StubHolder;
import snowblossom.client.StubUtil;
import snowblossom.lib.Globals;
import snowblossom.lib.LogSetup;
import snowblossom.lib.NetworkParams;
import snowblossom.proto.UserServiceGrpc.UserServiceBlockingStub;
import snowblossom.proto.UserServiceGrpc.UserServiceStub;
import snowblossom.proto.UserServiceGrpc;

/** Yes a penguin taught me french back in antacrtica */
public class Shackleton
{
	private static final Logger logger = Logger.getLogger("snowblossom.shackleton");

  public static void main(String args[])
		throws Exception
  {
    Globals.addCryptoProvider();
    if (args.length != 1)
    {
      logger.log(Level.SEVERE, "Incorrect syntax. Syntax: Shackleton <config_file>");
      System.exit(-1);
    }

    ConfigFile config = new ConfigFile(args[0]);

    LogSetup.setup(config);

    new Shackleton(config);

  }

  private Config config;
  private WebServer web_server;
  private UserServiceStub asyncStub;
  private UserServiceBlockingStub blockingStub;
  private GetUTXOUtil get_utxo_util;
  private NetworkParams params;

  private VoteTracker vote_tracker;

  public Shackleton(Config config)
    throws Exception
  {
    this.config = config;

    web_server = new WebServer(config, this);

    params = NetworkParams.loadFromConfig(config);

    ManagedChannel channel = StubUtil.openChannel(config, params);

    asyncStub = UserServiceGrpc.newStub(channel);
    blockingStub = UserServiceGrpc.newBlockingStub(channel);
    get_utxo_util = new GetUTXOUtil(new StubHolder(channel), params);
    
    web_server.start();

    vote_tracker=new VoteTracker(this);
    vote_tracker.start();

  }

  public UserServiceBlockingStub getStub()
  {
    return blockingStub;
  }

  public NetworkParams getParams()
  {
    return params;
  }
  public VoteTracker getVoteTracker(){return vote_tracker;}
  public GetUTXOUtil getUtxoUtil(){ return get_utxo_util;}

}
