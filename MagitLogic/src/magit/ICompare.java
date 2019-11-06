package magit;

public interface ICompare<F, S, R> {
    R compare(F i_First, S i_Second);
}
