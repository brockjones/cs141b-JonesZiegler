package edu.caltech.cs141b.hw2.gwt.collab.server;

import java.util.List;
// import java.util.logging.Logger; // Commented out since we don't use this
import java.util.Date;
import java.util.LinkedList;

import edu.caltech.cs141b.hw2.gwt.collab.client.CollaboratorService;
import edu.caltech.cs141b.hw2.gwt.collab.shared.DocumentMetadata;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockExpired;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockUnavailable;
import edu.caltech.cs141b.hw2.gwt.collab.shared.LockedDocument;
import edu.caltech.cs141b.hw2.gwt.collab.shared.UnlockedDocument;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class CollaboratorServiceImpl extends RemoteServiceServlet implements
		CollaboratorService {
	
	//private static final Logger log = Logger.getLogger(CollaboratorServiceImpl.class.toString()); //Commented out because we don't use it
	public List<DocumentMetadata> metadataList = new LinkedList<DocumentMetadata>(); // List of metadata
	public List<LockedDocument> lockedList = new LinkedList<LockedDocument>(); // List of Locked Documents
	public List<UnlockedDocument> unlockedList = new LinkedList<UnlockedDocument>(); // List of Unlocked Documents
	public static int lockCount = 0; // Use this to identify locked items. It is incremented with each use so each ID is (within reason) unique.

	
	/* This function returns the list of metadata 
	 * Arguments: None
	 * Return: LinkedList<DocumentMetadata>*/
	@Override
	public List<DocumentMetadata> getDocumentList() {
		return metadataList;
	}

	
	/* This function takes a key and locks a document if the document is available
	 * Arguments: String (serves as a key) 
	 * Returns: LockedDocument */
	@Override
	public LockedDocument lockDocument(String documentKey)
			throws LockUnavailable {
		UnlockedDocument current; // Used to iterate
		Date curDate; // Used for the lockedUntil
		LockedDocument newItem = null; // Return value
		/* This loop checks the unlocked documents for the matching key */
		for (int i=0; i<unlockedList.size(); i++){
			current = unlockedList.get(i);
			if (current.getKey().equals(documentKey)){
				/* Need to put ID in first coordinate */
				curDate = new Date();
				curDate.setMinutes(curDate.getMinutes()+10);
				/* We create the new LockedDocument */
				newItem = new LockedDocument(Integer.toString(lockCount), curDate, current.getKey(), current.getTitle(), current.getContents());
				lockCount++; /* Value is incremented to maintain uniqueness */
				unlockedList.remove(i);
				lockedList.add(0, newItem);
				break;
			}
		}
		if (newItem == null){
		throw new LockUnavailable("Item not found");
		}
		return newItem;
	}

	/* This function takes a document key and returns the document (if found) in read only 
	 * Arguments: String
	 * Returns: UnlockedDocument */
	@Override
	public UnlockedDocument getDocument(String documentKey) {
		UnlockedDocument current; // Iterates through list
		UnlockedDocument newItem = null; // Return variable
		/* Iterates through unlocked list checking if keys match */
		for (int i=0; i<unlockedList.size(); i++){
			current = unlockedList.get(i);
			if (current.getKey().equals(documentKey)){
				newItem = new UnlockedDocument(current.getKey(), current.getTitle(), current.getContents());
				break;
			}
		}
		return newItem;
	}

	/* This function takes a LockedDocument and if the key and lockedBy ID matches that of the document in 
	 * the list, unlocks the document and puts it in the unlocked list. If no key matches the LockedDocuments'
	 * key, then this saves a new document in the unlocked list.
	 * Arguments: LockedDocument
	 * Returns: UnlockedDocument */
	@Override
	public UnlockedDocument saveDocument(LockedDocument doc)
			throws LockExpired {
		LockedDocument currentLocked; // Iterates through list of LockedDocuments
		UnlockedDocument currentUnlocked; // Iterates through list of UnlockedDocuments
		UnlockedDocument newItem = null; // Return variable
		String lockId = doc.getLockedBy(); // lockedBy identifier
		boolean notInList = true; // This remains true only if the key is new
		
		/* This loop searches the list of UnlockedDocuments for the key. If the key is found, then we know the
		 * lock has expired. */
		for (int i=0; i<unlockedList.size(); i++){
			currentUnlocked = unlockedList.get(i);				
			if (currentUnlocked.getKey().equals(doc.getKey())){
				throw new LockExpired("Document is unlocked");
				}
			}
		/* Since the key was not in UnlockedDocuments, we know it is either new or currently locked */
		newItem = new UnlockedDocument(doc.getKey(), doc.getTitle(), doc.getContents());
		/* This loop searches the list of LockedDocuments for the key and checks the lockedBy ID. If the ID
		 * matches, then we unlock the document. If the ID doesn't match, then the lock has expired. If the
		 * ID is not found, then the key is new */
		for (int i=0; i<lockedList.size(); i++){
			currentLocked = lockedList.get(i);
			if (currentLocked.getKey().equals(doc.getKey())){
				if (!currentLocked.getLockedBy().equals(lockId)){
					throw new LockExpired("Invalid Lock ID");
				}
				lockedList.remove(i);
				unlockedList.add(0, newItem);
				notInList = false;
				break;
			}
		}
		/* If notInList is true, then we have a new key and must add to the metadata list */
		if (notInList){
			DocumentMetadata newMetadata = new DocumentMetadata(doc.getKey(), doc.getTitle());
			metadataList.add(0, newMetadata);
		}
		return newItem;
	}
	
	/* This function takes a LockedDocument, checks that it is still locked with the same id
	 * and if so, returns it to the list of UnlockedDocuments */
	@Override
	public void releaseLock(LockedDocument doc) throws LockExpired {
		LockedDocument currentLocked; // Iterates through the list
		boolean notInList = true; // Remains true only if the key is not found
		UnlockedDocument newItem; // Stores new UnlockedDocument
		String lockId = doc.getLockedBy(); // value in lockedBy field
		/* This loop looks through the list of LockedDocuments and checks if the key and ID 
		 * match. If so, we unlock the document without changing contents */
		for (int i=0; i<lockedList.size(); i++){
			currentLocked = lockedList.get(i);
			if (currentLocked.getKey().equals(doc.getKey())){
				if (!currentLocked.getLockedBy().equals(lockId)){
					throw new LockExpired("Invalid Lock ID");
				}
				newItem = new UnlockedDocument(currentLocked.getKey(), currentLocked.getTitle(), currentLocked.getLockedBy());
				lockedList.remove(i);
				unlockedList.add(0, newItem);
				notInList = false;
				break;
			}
		}
		/* notInList is true only if the item is not found */
		if (notInList){
			throw new LockExpired("Document not locked");
		}
		return;
	}

}

