import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;
import src.HashFileManager;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Created by shomakhov on 09.08.2017.
 */
public class Commands {
    static Collection<ProposalResponse> responses;
    static Collection<ProposalResponse> successful = new LinkedList<>();
    static FCUser org1_user = Main.org1_peer_admin;

    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Reader pemReader = new StringReader(new String(data));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);

        return privateKey;
    }


    private static BlockEvent.TransactionEvent invokeChaincode(HFClient client, Channel channel, ChaincodeID chaincodeID, String method, String[] args) throws Exception {

        Collection<ProposalResponse> successful = new LinkedList<>();

        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn(method);
        transactionProposalRequest.setProposalWaitTime(120000);
        transactionProposalRequest.setArgs(args);

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
        transactionProposalRequest.setTransientMap(tm2);

        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
            }
        }

        Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
        if (proposalConsistencySets.size() != 1) {
            System.out.println(format("Expected only one set of consistent proposal responses but got %d", proposalConsistencySets.size()));
        }


        ProposalResponse resp = transactionPropResp.iterator().next();
        byte[] x = resp.getChaincodeActionResponsePayload(); // This is the data returned by the chaincode.
        String resultAsString = null;
        if (x != null) {
            resultAsString = new String(x, "UTF-8");
        }

        System.out.println(resp.getChaincodeActionResponseStatus() + ": " + resultAsString);

        ChaincodeID cid = resp.getChaincodeID();

        return channel.sendTransaction(successful).get(120, TimeUnit.SECONDS);
    }


    //
//
//
    public static void sendTransInit() {

        try {

            InstantiateProposalRequest instantiateProposalRequest = Main.client.newInstantiationProposalRequest();
            instantiateProposalRequest.setUserContext(org1_user);
            instantiateProposalRequest.setProposalWaitTime(120000);
            instantiateProposalRequest.setChaincodeID(Main.chaincodeID);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[]{"a", "500", "b", "200"});

            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            instantiateProposalRequest.setChaincodeEndorsementPolicy(Main.chaincodeEndorsementPolicy);

            Map<String, byte[]> tmap = new HashMap<>();
            tmap.put("test", "data".getBytes());
            instantiateProposalRequest.setTransientMap(tmap);

            responses = Main.channel.sendInstantiationProposal(instantiateProposalRequest, Main.channel.getPeers());

            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                }
                System.out.println(response.getStatus());
                System.out.println(response.getMessage());
            }


        } catch (Exception e) {
            except(e);
            System.out.println(e.getMessage());
        }

    }

    public static void sendTransAdd(String fileName) throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("Зашли в ADD");
        try {
            Collection<Orderer> orderers = Main.channel.getOrderers();
//            Main.channel.sendTransaction(successful, orderers).thenApply(transactionEvent -> {
//
//                if (transactionEvent.isValid()) {
//                    try {
//                        Main.client.setUserContext(org1_user);
//                        return invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "add", new String[]{"doc0", "hash0"});
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                return null;
//
//            }).thenApply(transactionEvent -> {
//
//                if (transactionEvent.isValid()) {
//                    try {
//                        Main.client.setUserContext(org1_user);
//                        return invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "update", new String[]{"doc0", "hash0", "hash1"});
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                return null;
//
//            }).thenApply(transactionEvent -> {
//
//                if (transactionEvent.isValid()) {
//                    try {
//                        Main.client.setUserContext(org1_user);
//                        return invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "query", new String[]{"doc0"});
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                return null;
//
//            }).exceptionally(e -> {
//                if (e instanceof TransactionEventException) {
//                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
//                    if (te != null) {
//                        System.out.println(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
//                    }
//                }
//                System.out.println(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));
//
//                return null;
//            }).get(60, TimeUnit.SECONDS);
            Main.channel.sendTransaction(successful, orderers).thenApply(transactionEvent -> {

                if (transactionEvent.isValid()) {
                    try {
                        Main.client.setUserContext(org1_user);
                        return invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "add", new String[]{fileName, HashFileManager.getFilehash(fileName)});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                return null;
            }).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            except(e);
        }

        System.out.println("Выходим из ADD");

    }

    public static void sendTransUpdate(String fileName) {
        System.out.println("Зашли в UPDATE");
        try {
            TransactionProposalRequest queryByChaincodeRequest = Main.client.newTransactionProposalRequest();
            String oldHash = getOldHash(fileName);
            String newHash = HashFileManager.getFilehash(fileName);
            System.out.println("new hash : "+newHash);
            queryByChaincodeRequest.setArgs(new String[]{fileName,oldHash , newHash});
            queryByChaincodeRequest.setProposalWaitTime(120000);
            queryByChaincodeRequest.setFcn("update");
            queryByChaincodeRequest.setChaincodeID(Main.chaincodeID);

            Collection<ProposalResponse> queryProposals;
            queryProposals = Main.channel.sendTransactionProposal(queryByChaincodeRequest,Main.channel.getPeers());
            ProposalResponse response = queryProposals.iterator().next();
            System.out.println(response.getStatus() + " : " + response.getMessage());

            Main.channel.sendTransaction(queryProposals).get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            except(e);
        }

//
//        try {
//            Collection<Orderer> orderers = Main.channel.getOrderers();
//
//            Main.channel.sendTransaction(successful, orderers).thenApply(transactionEvent -> {
//
//               if (transactionEvent.isValid()) {
//                    try {
//                        Main.client.setUserContext(org1_user);
//                        return invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "update", new String[]{"doc0", "hash0", "hash1"});
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//               }
//               return null;
//
//            }).get(60, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            e.printStackTrace();
//            except(e);
//        }
        System.out.println("Вышли из UPDATE");
    }


    public static void sendTransQuery() {
        System.out.println("Зашли в QUERY");
//        try {
//            Collection<Orderer> orderers = Main.channel.getOrderers();
//
//            Main.channel.sendTransaction(successful, orderers).thenApply(transactionEvent -> {
//
//                if (transactionEvent.isValid()) {
//                    try {
//                        Main.client.setUserContext(org1_user);
//                        return invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "query", new String[]{"doc0"});
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                return null;
//
//            }).get(60, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            e.printStackTrace();
//            except(e);
//
//        }
        try {
            QueryByChaincodeRequest queryByChaincodeRequest = Main.client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(new String[]{"d:\\temp\\1.txt"});
            queryByChaincodeRequest.setFcn("query");
            queryByChaincodeRequest.setChaincodeID(Main.chaincodeID);

            Collection<ProposalResponse> queryProposals;


            queryProposals = Main.channel.queryByChaincode(queryByChaincodeRequest, Main.channel.getPeers());
            ProposalResponse response = queryProposals.iterator().next();
            System.out.println(response.getStatus() + " : " + response.getMessage());
            String result = new String(response.getChaincodeActionResponsePayload(), "UTF-8");
            System.out.println(result);


        } catch (Exception e) {
            except(e);
        }


        System.out.println("Вышли из QUERY");
    }

    public static String getOldHash(String fileName) {
        System.out.println("Зашли в GET");
        try {

            TransactionProposalRequest queryByChaincodeRequest = Main.client.newTransactionProposalRequest();
            queryByChaincodeRequest.setProposalWaitTime(12L);
            queryByChaincodeRequest.setArgs(new String[]{fileName});
            queryByChaincodeRequest.setFcn("get");
            queryByChaincodeRequest.setChaincodeID(Main.chaincodeID);

            Collection<ProposalResponse> queryProposals;
            queryProposals = Main.channel.sendTransactionProposal(queryByChaincodeRequest,Main.channel.getPeers());
            ProposalResponse response = queryProposals.iterator().next();

            System.out.println(response.getStatus() + " : " + response.getMessage());
            String result = new String(response.getChaincodeActionResponsePayload(), "UTF-8");
            System.out.println(result);
            return result;

        } catch (Exception e) {
            except(e);
        }


        System.out.println("Выходим из GET");
        return "Что-то пошло не так";

    }


    private static void except(Exception e) {
        //  System.out.println(e.getMessage());
        if (e instanceof TransactionEventException) {
            BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
            if (te != null) {
                System.out.println(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
            }
        }
        System.out.println(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));


    }


}



