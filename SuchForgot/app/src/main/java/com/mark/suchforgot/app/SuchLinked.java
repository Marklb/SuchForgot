package com.mark.suchforgot.app;


public class SuchLinked{

    /*
        A LinkedList I made to test different ways of handling the
        accelerometer data that is constantly being collected.

        This class wasn't meant to be the actual list we used, but
        the app worked with this one implemented so we just left it.
     */



    Node head;
    Node center;
    Node tail;
    private int size;

    /*
     * creates an empty list
     */
    public SuchLinked() {
        clear();
    }

    /*
     * remove all elements in the list
     */
    public final synchronized void clear() {
        head = null;
        center = null;
        tail = null;
        size = 0;
    }

    /*
     * returns true if this container is empty.
     */
    public final boolean isEmpty() {
        return (head == null);
    }

    /*
     * Return an iterator positioned at the head.
     */
    public final SuchIterator head() {
        return new SuchIterator(this, head);
    }

    public final int size(){
        return size;
    }

    public final synchronized void add(Double obj){
        Node item = new Node(obj);
        if(head == null){
            head = item;
            center = item;
            tail = item;
            size++;
        }else{
            tail.next = item;
            Node temp = tail;
            tail = item;
            tail.previous = temp;
            size++;
            if((size % 2) == 0){
                center = center.next;
            }
        }
    }

    public void printAll(){
        Node curr = head;
        int inc = 0;
        while(curr != null){
            System.out.println("["+inc+"] "+curr.obj);
            curr = curr.next;
            inc++;
        }
    }

    public Double getCenter(){
        return center.obj;
    }

    public synchronized void removeFirstHalf(){
        if(size <= 1)return;
        head = center;
        if(size%2 == 0){
            size = size/2;
        }else{
            size = (size/2)+1;
        }
        Node temp = head;
        int count = 0;
        while(count < (size/2)){
            temp = temp.next;
            count++;
        }
        center = temp;
    }

    //////////////////////////////////////////////
    // Item Node
    //////////////////////////////////////////////
    final class Node {
        Double obj;
        Node previous, next;

        public Node(Double obj) {
            this(null, obj, null);
        }

        public Node(Node previous, Double obj, Node next) {
            this.previous = previous;
            this.obj = obj;
            this.next = next;
        }
    }

    //////////////////////////////////////////////
    // Iterator
    //////////////////////////////////////////////
    public final class SuchIterator {
        SuchLinked owner;
        Node pos;

        SuchIterator(SuchLinked owner, Node pos){
            this.owner = owner;
            this.pos = pos;
        }

        /*
         * move to head position
         */
        public void head() {
            pos = owner.head;
        }

        /*
         * move to next position
         */
        public Double next() {
            Double toReturn = pos.obj;
            pos = pos.next;
            return toReturn;
        }

        /*
         * move to previous position
         */
        public void previous() {
            pos = pos.previous;
        }

        public boolean hasNext(){
            return (pos != null);
        }
    }
}
