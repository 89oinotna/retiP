package server;

public class DoubleVal<V,Z> {
        V first;

        public Z getSecond() {
            return second;
        }

        public DoubleVal<V,Z> setSecond(Z second) {
            this.second = second;
            return this;
        }

        Z second;

        public V getFirst() {
            return first;
        }

        public DoubleVal<V,Z> setFirst(V first) {
            this.first = first;
            return this;
        }

        DoubleVal(V first, Z second){
            this.first=first;
            this.second=second;
        }

}
