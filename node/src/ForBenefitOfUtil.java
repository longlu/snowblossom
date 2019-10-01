package snowblossom.node;
import com.google.common.collect.TreeMultimap;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import snowblossom.lib.*;
import snowblossom.lib.db.DB;
import snowblossom.lib.db.DBMapMutationSet;
import snowblossom.lib.trie.HashedTrie;
import snowblossom.proto.*;
import snowblossom.trie.proto.TrieNode;

public class ForBenefitOfUtil
{
  public static final ByteString FBO_MAP_PREFIX = ByteString.copyFrom("fbo2".getBytes());
  public static final ByteString ID_MAP_PREFIX = ByteString.copyFrom("id2o".getBytes());
  public static final ByteString ID_MAP_USER = ByteString.copyFrom("user".getBytes());
  public static final ByteString ID_MAP_CHAN = ByteString.copyFrom("chan".getBytes());


  public static void saveIndex(Block blk, Map<ByteString, ByteString> update_map)
  {
    int height = blk.getHeader().getBlockHeight();

    ChainHash blk_id = new ChainHash(blk.getHeader().getSnowHash());

    for(Transaction tx : blk.getTransactionsList())
    {
      TransactionInner inner = TransactionUtil.getInner(tx);

      int out_idx = 0;
      for(TransactionOutput out : inner.getOutputsList())
      {
        ChainHash tx_id = new ChainHash(tx.getTxHash());
        if (out.getForBenefitOfSpecHash().size() == Globals.ADDRESS_SPEC_HASH_LEN)
        {
          ByteString for_addr = out.getForBenefitOfSpecHash();

          ByteString key = getKey(for_addr, tx_id, out_idx);

          update_map.put(key, out.toByteString());
        }
        if (out.getIds().getUsername().size() > 0)
        {
          ByteString key = getIdKey(ID_MAP_USER, out.getIds().getUsername(), height, tx_id, out_idx);
          update_map.put(key, out.toByteString());
        }
        if (out.getIds().getChannelname().size() > 0)
        {
          ByteString key = getIdKey(ID_MAP_CHAN, out.getIds().getChannelname(), height, tx_id, out_idx);
          update_map.put(key, out.toByteString());
        }
        out_idx++;
      }
    }
  }

  public static ByteString getKey(ByteString for_addr, ChainHash tx_id, int out_idx)
  {
    byte[] buff = new byte[Globals.ADDRESS_SPEC_HASH_LEN + Globals.BLOCKCHAIN_HASH_LEN + 4];

    ByteBuffer bb = ByteBuffer.wrap(buff);
    bb.put(for_addr.toByteArray());
    bb.put(tx_id.toByteArray());
    bb.putInt(out_idx);

    return FBO_MAP_PREFIX.concat(ByteString.copyFrom(buff));
  }

  public static ByteString getIdKey(ByteString type, ByteString name, int height, ChainHash tx_id, int out_idx)
  {
    ByteString name_hash = DigestUtil.hash(name);

    return ID_MAP_PREFIX
      .concat(type)
      .concat(name_hash)
      .concat(getIntString(height))
      .concat(tx_id.getBytes())
      .concat(getIntString(out_idx));
  }


  public static ByteString getIntString(int x)
  {
    byte[] buff = new byte[4];
    ByteBuffer bb = ByteBuffer.wrap(buff);
    bb.putInt(x);
    ByteString str = ByteString.copyFrom(buff);
    return str;
  }


  public static ByteString getValue(ChainHash tx_id, int out_idx)
  {
    byte[] buff = new byte[Globals.BLOCKCHAIN_HASH_LEN + 4];

    ByteBuffer bb = ByteBuffer.wrap(buff);
    bb.put(tx_id.toByteArray());
    bb.putInt(out_idx);

    return ByteString.copyFrom(buff);
  }

  protected static TxOutPoint getOutpoint(ByteString tx_info, ByteString val)
  {

    if (tx_info.size() != Globals.BLOCKCHAIN_HASH_LEN + 4)
    {
      throw new RuntimeException("Wrong size tx_info");
    }
    
    TxOutPoint.Builder op = TxOutPoint.newBuilder();

    op.setTxHash( tx_info.substring(0, Globals.BLOCKCHAIN_HASH_LEN));
    op.setOutIdx( tx_info.substring(Globals.BLOCKCHAIN_HASH_LEN).asReadOnlyByteBuffer().getInt() );

    try
    {
      op.setOut( TransactionOutput.parseFrom(val) );
    }
    catch(com.google.protobuf.InvalidProtocolBufferException e)
    {
      throw new RuntimeException(e);
    }

    return op.build();
    

  }

  /**
   * Return a list of outputs that are marked in favor of 'spec_hash'
   */
  public static TxOutList getFBOList(AddressSpecHash spec_hash, HashedTrie db, ByteString trie_root)
  {
    TxOutList.Builder list = TxOutList.newBuilder();

    ByteString search_key = FBO_MAP_PREFIX.concat(spec_hash.getBytes()); 
    
    TreeMap<ByteString, ByteString> map = db.getDataMap(trie_root, search_key, 10000);

    for(Map.Entry<ByteString, ByteString> me : map.entrySet())
    {
      ByteString tx_info = me.getKey().substring( search_key.size() );

      list.addOutList( getOutpoint(tx_info, me.getValue()));
    }
    return list.build();
  }

  /**
   * returns a list of outputs that are marked with the name 'name' in ascending
   * order of confirmation height.  So oldest first.
   */
  public static TxOutList getIdList(ByteString type, ByteString name, HashedTrie db, ByteString trie_root)
  {
    ByteString name_hash = DigestUtil.hash(name);

    TxOutList.Builder list = TxOutList.newBuilder();
    ByteString search_key = ID_MAP_PREFIX
      .concat(type)
      .concat(name_hash);

    TreeMap<ByteString, ByteString> map = db.getDataMap(trie_root, search_key, 10000);

    for(Map.Entry<ByteString, ByteString> me : map.entrySet())
    {
      ByteString tx_info = me.getKey().substring( search_key.size() + 4 );

      list.addOutList( getOutpoint(tx_info, me.getValue()));
    }

    return list.build();
  }

  public static TxOutList getIdListUser(ByteString name, HashedTrie db, ByteString trie_root)
  {
    return getIdList(ID_MAP_USER, name, db, trie_root);
  }

  public static TxOutList getIdListChannel(ByteString name, HashedTrie db, ByteString trie_root)
  {
    return getIdList(ID_MAP_CHAN, name, db, trie_root);
  }


  /*public static HistoryList getHistory(AddressSpecHash spec_hash, DB db, BlockSummary summary)
    throws ValidationException
  {
    
    ByteString key = MAP_PREFIX.concat(spec_hash.getBytes());

    LinkedList<TrieNode> proof = new LinkedList<>();
    LinkedList<TrieNode> results = new LinkedList<>();
    db.getChainIndexTrie().getNodeDetails(
      summary.getChainIndexTrieHash(),
      key,
      proof,
      results,
      Globals.ADDRESS_HISTORY_MAX_REPLY);

    
    //List<ByteString> value_set = db.getAddressHistoryMap().getSet(spec_hash.getBytes(), Globals.ADDRESS_HISTORY_MAX_REPLY);

    HistoryList.Builder hist_list = HistoryList.newBuilder();

    for(TrieNode node : results)
    {
      if (node.getIsLeaf())
      {
        ByteString val = node.getLeafData();
        ByteBuffer bb = ByteBuffer.wrap(val.toByteArray());
        int height = bb.getInt();

        byte[] b = new byte[Globals.BLOCKCHAIN_HASH_LEN];

        bb.get(b);
        ChainHash blk_hash = new ChainHash(b);

        bb.get(b);
        ChainHash tx_hash = new ChainHash(b);

        //if (blk_hash.equals(cache.getHash(height)))
        {
          hist_list.addEntries(HistoryEntry
            .newBuilder()
            .setBlockHeight(height)
            .setTxHash(tx_hash.getBytes())
            .setBlockHash(blk_hash.getBytes())
            .build());
        }
      }
    }

    return hist_list.build();
  }*/

}