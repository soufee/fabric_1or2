package main

import (
	"bytes"
	"fmt"
	"strconv"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)



// DocHash Chaincode implementation
type DocChaincode struct {
}



var logger = shim.NewLogger("DocChaincode")

func (t *DocChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {

	logger.Info("Init")

	return shim.Success(nil)
}

func (t *DocChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {

	logger.Info("Invoke")

	function, args := stub.GetFunctionAndParameters()

	logger.Info(function, args)

	if function == "add" {
		// Add document, parameters: name, current_hash
		return t.add(stub, args)
	} else if function == "update" {
		// Update document, parameters: name, previous_hash, current_hash
		return t.update(stub, args)
	} else if function == "get" {
		// Get document, parameters: name
		return t.get(stub, args)
	} else if function == "query" {
		// Get history of document changes, parameters: name
		return t.query(stub, args)
	}


	logger.Info("Invoke error.")

	return shim.Error("Invalid invoke function name. Expecting \"add\" \"update\" \"query\".")
}

// Add puts document into KVS. In this new file secreteWord attached
// for protect file from edition of other user.
//
// parameters: name, current_hash, secretWord.
func (t *DocChaincode) add(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	logger.Info("Add started.")

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting name of document and its hash.")
	}

	var Name, Hash, SecretWord string

	Name = args[0]
	Hash = args[1]
	SecretWord = args[2]

	byteStream, err := stub.GetState(Name)


	if err != nil {
		return shim.Error(err.Error())
	}

	if byteStream != nil {
		var fileContent = getFileContentFromStreamBytes(byteStream)
		return shim.Error("File \"" + Name + "\" already exists with hash=" + string(fileContent.Hash))
	}

	var newFileCont = FileContent{Hash: []byte(Hash), SecretWord: SecretWord}
	var newFileContStream = getByteStreamFromFileContent(newFileCont)
	err = stub.PutState(Name, newFileContStream)
	if err != nil {
		return shim.Error(err.Error())
	}


	logger.Info("Add finished.")

	return shim.Success(nil)
}

// update puts new document structure into KVS.
// Structure contain from hash value and secretWord that secure this file
// from update of other user that don't add this file.
//
// SecreteWord need for authentification purposes.
//
// parameters: name, current_hash,
func (t *DocChaincode) update(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	logger.Info("Update started.")

	if len(args) != 4 {
		return shim.Error("Incorrect number of arguments. Expecting name of document and its previous hash and new hash.")
	}

	var Name, OldHash, NewHash, SecreteWord string

	Name = args[0]
	OldHash = args[1]
	NewHash = args[2]
	SecreteWord = args[3]

	byteStream, err := stub.GetState(Name)

	if err != nil {
		return shim.Error(err.Error())
	}

	if byteStream == nil {
		return shim.Error("File \"" + Name + "\" doesnt exist in storage.")
	}

	var fileCont = getFileContentFromStreamBytes(byteStream)
	if string(fileCont.Hash) != OldHash {
		return shim.Error("File \"" + Name + "\" has another hash=" + string(fileCont.Hash))
	}

	if fileCont.SecretWord != SecreteWord {
		return shim.Error("Acces denied for file \"" + Name + "\". There is no rights to edit this file. Wrong SecreteCode for this file.")
	}

	var newFileContent = FileContent{Hash: []byte(NewHash), SecretWord: SecreteWord}
	var newByteStream = getByteStreamFromFileContent(newFileContent)
	err = stub.PutState(Name, newByteStream)
	if err != nil {
		return shim.Error(err.Error())
	}

	logger.Info("Update finished.")

	return shim.Success(nil)
}

// Get returns document hash from KVS.
// All users can get hash of file.
//
// parameters: name.
func (t *DocChaincode) get(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	logger.Info("Get started.")

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting name of document.")
	}

	var Name string
	Name = args[0]

	byteStream, err := stub.GetState(Name)

	if err != nil {
		return shim.Error(err.Error())
	}

	if byteStream == nil {
		return shim.Error("File \"" + Name + "\" doesnt exist in storage.")
	}

	var fileContent = getFileContentFromStreamBytes(byteStream)
	logger.Info("Get finished:", string(fileContent.Hash))

	return shim.Success(fileContent.Hash)
}

// Query gets history of document changes from KVS.
// All users can get query from all files without restriction.
//
// parameters: name.
func (t *DocChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {

	logger.Info("Query started.")

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting name of document.")
	}

	var Name string
	var err error

	Name = args[0]

	resultsIterator, err := stub.GetHistoryForKey(Name)
	if err != nil {
		shim.Error(err.Error())
	}
	defer resultsIterator.Close()

	// buffer is a JSON array containing historic values
	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		response, err := resultsIterator.Next()
		if err != nil {
			shim.Error(err.Error())
		}

		// Add a comma before array members, suppress it for the first array member
		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}
		buffer.WriteString("{\"TxId\":")
		buffer.WriteString("\"")
		buffer.WriteString(response.TxId)
		buffer.WriteString("\"")

		buffer.WriteString(", \"Value\":")
		buffer.WriteString("\"")
		// If it was a delete operation on given key, then we need to set the corresponding value null.
		// Else, we will write the response.Value as-is
		if response.IsDelete {
			buffer.WriteString("null")
		} else {
			var fileContent = getFileContentFromStreamBytes(response.Value)
			buffer.WriteString(string(fileContent.Hash))
		}
		buffer.WriteString("\"")

		buffer.WriteString(", \"Timestamp\":")
		buffer.WriteString("\"")
		buffer.WriteString(time.Unix(response.Timestamp.Seconds, int64(response.Timestamp.Nanos)).String())
		buffer.WriteString("\"")

		buffer.WriteString(", \"IsDelete\":")
		buffer.WriteString("\"")
		buffer.WriteString(strconv.FormatBool(response.IsDelete))
		buffer.WriteString("\"")

		buffer.WriteString("}")
		bArrayMemberAlreadyWritten = true
	}

	buffer.WriteString("]")

	logger.Info("Query finished:", buffer.String())

	return shim.Success(buffer.Bytes())
}

func main() {
	logger.Info("main")
	err := shim.Start(new(DocChaincode))
	if err != nil {
		fmt.Printf("Error starting doc chaincode: %s", err)
	}
}