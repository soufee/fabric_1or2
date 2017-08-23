package main

import (
	"fmt"
	"testing"

	"github.com/hyperledger/fabric/core/chaincode/shim"
)

//--tags nopkcs11



func checkInit(t *testing.T, stub *shim.MockStub, args [][]byte) {
	res := stub.MockInit("1", nil)
	if res.Status != shim.OK {
		fmt.Println("Init failed", string(res.Message))
		t.FailNow()
	}
}

func checkAdd(t *testing.T, stub *shim.MockStub, name string, hash string, secreteWord string) {
	res := stub.MockInvoke("1", [][]byte{[]byte("add"), []byte(name), []byte(hash), []byte(secreteWord)})
	if res.Status != shim.OK {
		fmt.Println("Add failed", string(res.Message))
		t.FailNow()
	}
}

func checkUpdate(t *testing.T, stub *shim.MockStub, name string, oldhash string, newhash string, secretword string) {
	res := stub.MockInvoke("1", [][]byte{[]byte("update"), []byte(name), []byte(oldhash), []byte(newhash), []byte(secretword)})
	if res.Status != shim.OK {
		fmt.Println("Update failed", string(res.Message))
		if oldhash != newhash {
			t.FailNow()
		}

	}
}

func checkGet(t *testing.T, stub *shim.MockStub, name string) {
	res := stub.MockInvoke("1", [][]byte{[]byte("get"), []byte(name)})
	if res.Status != shim.OK {
		fmt.Println("Get failed", string(res.Message))
		t.FailNow()
	}
}

func checkState(t *testing.T, stub *shim.MockStub, name string, hash string, secretWord string) {
	bytes := stub.State[name]
	if bytes == nil {
		fmt.Println("State", name, "failed to get value")
		t.FailNow()
	}
	
	var fileContObj = getFileContentFromStreamBytes(bytes)

	if string(fileContObj.Hash) != hash {
		fmt.Println("Hash value in", name, "was not", hash, "as expected")
		t.FailNow()
	}
	if fileContObj.SecretWord != secretWord {
		fmt.Println("Secrete word value in", name, "was not", secretWord, "as expected")
		t.FailNow()
	}
}

func TestDoc_Add(t *testing.T) {
	fmt.Println("TestDoc_Add")

	chaincode := new(DocChaincode)
	stub := shim.NewMockStub("doc", chaincode)

	checkInit(t, stub, nil)
	checkAdd(t, stub, "file0001.txt", "hashvalue1", "SecreteWord")
	checkState(t, stub, "file0001.txt", "hashvalue1", "SecreteWord")
}

func TestDoc_Update(t *testing.T) {
	fmt.Println("TestDoc_Update")

	chaincode := new(DocChaincode)
	stub := shim.NewMockStub("doc", chaincode)

	checkInit(t, stub, nil)
	checkAdd(t, stub, "file0001.txt", "hashvalue1", "SecreteWord")
	checkUpdate(t, stub, "file0001.txt", "hashvalue1", "hashvalue2", "SecreteWord")
	checkState(t, stub, "file0001.txt", "hashvalue2", "SecreteWord")
}

func TestDoc_UpdateEqual(t *testing.T) {
	fmt.Println("TestDoc_UpdateEqual")

	chaincode := new(DocChaincode)
	stub := shim.NewMockStub("doc", chaincode)

	checkInit(t, stub, nil)
	checkAdd(t, stub, "file0001.txt", "hashvalue1", "SecreteWord")
	checkUpdate(t, stub, "file0001.txt", "hashvalue1", "hashvalue1", "SecreteWord")
}

func TestDoc_UpdateWithDifferentSecretWord(t *testing.T)  {
	fmt.Println("TestDoc_UpdateWithDifferentSecretWord")

	chaincode := new(DocChaincode)
	stub := shim.NewMockStub("doc", chaincode)

	checkInit(t, stub, nil)
	checkAdd(t, stub, "file0001.txt", "hashvalue1", "SecreteWord")
	checkUpdate(t, stub, "file0001.txt", "hashvalue1", "hashvalue1", "SecreteWord2")
}

func TestDoc_Get(t *testing.T) {
	fmt.Println("TestDoc_Get")

	chaincode := new(DocChaincode)
	stub := shim.NewMockStub("doc", chaincode)

	checkInit(t, stub, nil)
	checkAdd(t, stub, "file0001.txt", "hashvalue1", "SecreteWord")
	checkGet(t, stub, "file0001.txt")
}

func TestDoc_Query(t *testing.T) {
	fmt.Println("TestDoc_Query: MockStub.GetHistoryForKey not implemented yet, so we can't test Query.")
}
