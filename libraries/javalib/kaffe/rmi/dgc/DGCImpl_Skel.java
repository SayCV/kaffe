// Skel class generated by rmic - DO NOT EDIT!

package kaffe.rmi.dgc;

public final class DGCImpl_Skel
    implements java.rmi.server.Skeleton
{
    private static final long interfaceHash = -669196253586618813L;
    
    private static final java.rmi.server.Operation[] operations = {
        new java.rmi.server.Operation("void clean(java.rmi.server.ObjID[], long, java.rmi.dgc.VMID, boolean"),
        new java.rmi.server.Operation("java.rmi.dgc.Lease dirty(java.rmi.server.ObjID[], long, java.rmi.dgc.Lease")
    };
    
    public java.rmi.server.Operation[] getOperations() {
        return ((java.rmi.server.Operation[]) operations.clone());
    }
    
    public void dispatch(java.rmi.Remote obj, java.rmi.server.RemoteCall call, int opnum, long hash) throws java.lang.Exception {
        if (opnum < 0) {
            if (hash == -5803803475088455571L) {
                opnum = 0;
            }
            else if (hash == -8139341527526761862L) {
                opnum = 1;
            }
            else {
                throw new java.rmi.server.SkeletonMismatchException("interface hash mismatch");
            }
        }
        else if (hash != interfaceHash) {
            throw new java.rmi.server.SkeletonMismatchException("interface hash mismatch");
        }
        
        kaffe.rmi.dgc.DGCImpl server = (kaffe.rmi.dgc.DGCImpl)obj;
        switch (opnum) {
        case 0:
        {
            java.rmi.server.ObjID[] $param_0;
            long $param_1;
            java.rmi.dgc.VMID $param_2;
            boolean $param_3;
            try {
                java.io.ObjectInput in = call.getInputStream();
                $param_0 = (java.rmi.server.ObjID[])in.readObject();
                $param_1 = (long)in.readLong();
                $param_2 = (java.rmi.dgc.VMID)in.readObject();
                $param_3 = (boolean)in.readBoolean();
                
            }
            catch (java.io.IOException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
            }
            catch (java.lang.ClassCastException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
            }
            finally {
                call.releaseInputStream();
            }
            server.clean($param_0, $param_1, $param_2, $param_3);
            try {
                java.io.ObjectOutput out = call.getResultStream(true);
            }
            catch (java.io.IOException e) {
                throw new java.rmi.MarshalException("error marshalling return", e);
            }
            break;
        }
        
        case 1:
        {
            java.rmi.server.ObjID[] $param_0;
            long $param_1;
            java.rmi.dgc.Lease $param_2;
            try {
                java.io.ObjectInput in = call.getInputStream();
                $param_0 = (java.rmi.server.ObjID[])in.readObject();
                $param_1 = (long)in.readLong();
                $param_2 = (java.rmi.dgc.Lease)in.readObject();
                
            }
            catch (java.io.IOException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
            }
            catch (java.lang.ClassCastException e) {
                throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
            }
            finally {
                call.releaseInputStream();
            }
            java.rmi.dgc.Lease $result = server.dirty($param_0, $param_1, $param_2);
            try {
                java.io.ObjectOutput out = call.getResultStream(true);
                out.writeObject($result);
            }
            catch (java.io.IOException e) {
                throw new java.rmi.MarshalException("error marshalling return", e);
            }
            break;
        }
        
        default:
            throw new java.rmi.UnmarshalException("invalid method number");
        }
    }
}
