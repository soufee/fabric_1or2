package main

import (
	"encoding/json"
	"fmt"
)




type FileContent struct{
	Hash []byte
	SecretWord string
}

// Convert FileContent structure to byte stream.
func getByteStreamFromFileContent(fileCont FileContent) ([]byte)  {
	res, err := json.Marshal(fileCont)
	if err != nil{
		fmt.Println("Error in marshalling: ", err)
	}

	return res
}

// Convert byte stream to FileContent.
func getFileContentFromStreamBytes(stream []byte) (FileContent)  {
	var fileCont FileContent
	err :=  json.Unmarshal(stream, &fileCont)
	if err != nil{
		fmt.Println("Error in umashalling: ", err)
	}

	return fileCont
}