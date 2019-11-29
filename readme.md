## ALM Concurrent Editor

#### API Description

Service provides 4 methods for operating on documents:

- `createDocument` - accepts document id, creates document with this id in repository
- `applyPatch` - accepts document id and patch (see below), applies patch to the document
- `getDocumentContent` - returns current document state and it's revision number
- `getDocumentDiff` - accepts document id and revision, returns difference between current and client revisions


Patch is and object containing its id (should be unique across document) and changes client wants to apply to the document

3 types of changes are supported:

- `AddLineUpdate(lineIndex)` - adds line at index
- `RemoveLineUpdate(lineIndex)` - removes line at index
- `ChangeLineUpdate(lineIndex, update)` - changes line at index
   - `update` can be `AddSymbols(index, symbols)` or `RemoveSymbols(fromIndex, toIndex)` 
   
Before saving client changes are merged with existing changes client did not know about.
Strategy is the following:

 1. We find missing updates using passed revision 
 1. Apply server `add` and `remove` line updates to client patches 
 1. Then we move server and client `change` updates to the same index space:
    - Apply server `add` and `remove` updates that were after server `change` update to it
    - Apply client `add` and `remove` updates to server `change` updates
    - Apply client `add` and `remove` updates that were after client `change` update to it
 1. As they are in the same index space now we can merge them 
 (refer to `PatchService:mergeLineUpdate` and `MergeLineUpdatesTest`)
 
Client logic would be the following
1. Collect updates locally
1. Send them to the server
1. After receiving diff update remove updates that were acknowledged by the server, 
update local document state and not sent updates using same logic as implemented on server
1. repeat
