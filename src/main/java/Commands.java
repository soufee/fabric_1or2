import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import src.HashFileManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * Created by shomakhov on 11.08.2017.
 */
public class Commands {
    static Collection<ProposalResponse> responses;
    static Collection<ProposalResponse> successful = new LinkedList<>();
    static FCUser peerAdmin = Main.org1_peer_admin;
    static BlockEvent.TransactionEvent transactionEvent;

    public static void sendTransInit() {
        //     Collection<ProposalResponse> successful = new LinkedList<>();
        try {

            InstantiateProposalRequest request = Main.client.newInstantiationProposalRequest();
            request.setUserContext(peerAdmin);
            request.setProposalWaitTime(120000);
            request.setChaincodeID(Main.chaincodeID);
            request.setFcn("init");
            request.setArgs(new String[]{"a", "500", "b", "200"});

            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            request.setTransientMap(tm);

            request.setChaincodeEndorsementPolicy(Main.chaincodeEndorsementPolicy);

            Map<String, byte[]> tmap = new HashMap<>();
            tmap.put("test", "data".getBytes());
            request.setTransientMap(tmap);

            responses = Main.channel.sendInstantiationProposal(request, Main.channel.getPeers());

            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                }
                System.out.println("Init status " + response.getStatus());
                System.out.println("Init message " + response.getMessage());
            }
            Collection<Orderer> orderers = Main.channel.getOrderers();
            transactionEvent = Main.channel.sendTransaction(successful, orderers).get();

        } catch (Exception e) {
            except(e);
            System.out.println(e.getMessage());
        }

    }


    private static void except(Exception e) {
        //  System.out.println(e.getMessage());
        if (e instanceof TransactionEventException) {
            BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
            if (te != null) {
                System.out.println(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
            }
        }
        if (e instanceof IOException) {
            System.out.println("Файл не найден или введенный путь не является файлом");
            return;
        }
        if (e instanceof InvalidArgumentException) {
            System.out.println("Переданные параметры не соответствуют типу команды. Возможно вы пытаетесь получить доступ к несуществующему файлу");
            return;
        }
        System.out.println(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));
    }

    private static BlockEvent.TransactionEvent invokeChaincode(HFClient client, Channel channel, ChaincodeID chaincodeID, String method, String[] args) throws Exception {

        Collection<ProposalResponse> successful = new LinkedList<>();

        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn(method);
        transactionProposalRequest.setProposalWaitTime(120000);
        transactionProposalRequest.setArgs(args);

//        Map<String, byte[]> tm2 = new HashMap<>();
//        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
//        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
//        tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
//        transactionProposalRequest.setTransientMap(tm2);

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
        byte[] x = resp.getChaincodeActionResponsePayload();
        String resultAsString = null;
        if (x != null) {
            resultAsString = new String(x, "UTF-8");
        }

        System.out.println(resp.getChaincodeActionResponseStatus() + ": " + resultAsString);


        return channel.sendTransaction(successful).get(10, TimeUnit.SECONDS);
    }

    public static void sendTransAdd(String fileName) {
        System.out.println("Зашли в ADD");
        try {
            Main.client.setUserContext(peerAdmin);
            transactionEvent = invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "add", new String[]{fileName, HashFileManager.getFilehash(fileName)});
        }
        catch (Exception e) {
            except(e);
        }
        System.out.println("Выходим из ADD");
    }

    public static void sendTransUpdate(String fileName) {
        System.out.println("Зашли в UPDATE");
        try {
            Main.client.setUserContext(peerAdmin);
            String oldHash = getOldHash(fileName);
            String newHash = HashFileManager.getFilehash(fileName);
            transactionEvent = invokeChaincode(Main.client, Main.channel, Main.chaincodeID, "update", new String[]{fileName, oldHash, newHash});
            getOldHash(fileName);
        } catch (Exception e) {
            except(e);
        }
        System.out.println("Выходим из UPDATE");
    }

    public static void sendTransQuery(String fileName) {
        System.out.println("Зашли в QUERY");

        try {
            QueryByChaincodeRequest queryByChaincodeRequest = Main.client.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(new String[]{fileName});
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
            queryProposals = Main.channel.sendTransactionProposal(queryByChaincodeRequest, Main.channel.getPeers());
            ProposalResponse response = queryProposals.iterator().next();

            System.out.println(response.getStatus() + " : " + response.getMessage());
            String result = new String(response.getChaincodeActionResponsePayload(), "UTF-8");
            System.out.println("Действующий хэш файла " + fileName + " : " + result);
            return result;
        } catch (Exception e) {
            except(e);
        }


        System.out.println("Выходим из GET");
        return "Что-то пошло не так";

    }

}
