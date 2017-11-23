package org.basex.query.func.fn;

import static org.basex.query.QueryError.*;

import java.math.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.query.value.type.SeqType.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnSum extends Aggr {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final Expr ex = exprs[0];
    if(ex instanceof RangeSeq || ex instanceof Range) {
      final Item it = gauss(ex.atomValue(qc, info));
      if(it != null) return it;
    } else {
      if(ex instanceof SingletonSeq) {
        final Item it = multiply((SingletonSeq) ex);
        if(it != null) return it;
      }
      final Iter iter = exprs[0].atomIter(qc, info);
      final Item it = iter.next();
      if(it != null) return sum(iter, it, false, qc);
    }

    // return default item
    return exprs.length == 2 ? exprs[1].atomItem(qc, info) : Int.ZERO;
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    final Expr ex1 = exprs[0], ex2 = exprs.length == 2 ? exprs[1] : Empty.SEQ;
    if(ex1 instanceof RangeSeq) return gauss((Value) ex1);
    if(ex1 instanceof SingletonSeq) {
      final Item it = multiply((SingletonSeq) ex1);
      if(it != null) return it;
    }

    // empty sequence: replace with default item
    final SeqType st1 = ex1.seqType(), st2 = ex2.seqType();
    if(st1.zero()) {
      // sequence is empty
      if(ex2 == Empty.SEQ) return ex1;
      if(st2.instanceOf(SeqType.ITR) && !ex1.has(Flag.NDT)) return ex2;
    } else if(st1.oneOrMore() && !st1.mayBeArray()) {
      // sequence is not empty: assign result type
      final Type t1 = st1.type;
      if(t1.isNumber()) exprType.assign(t1.seqType());
      if(t1.isUntyped()) exprType.assign(SeqType.DBL);
    } else if(!st2.zero() && !st2.mayBeArray()) {
      // sequence may be empty: include non-empty default argument in tests
      final Type t1 = st1.type, t2 = st2.type;
      final Type type = t1.isNumberOrUntyped() && t2.isNumberOrUntyped() ?
        Calc.type(t1, t2) : AtomType.AAT;
      exprType.assign(type, st2.one() ? Occ.ONE : Occ.ZERO_ONE);
    }
    return this;
  }

  /**
   * Little Gauss computation.
   * @param value sequence of dense and distinct numbers
   * @return sum, or {@code null} if sequence is empty
   * @throws QueryException query exception
   */
  private Item gauss(final Value value) throws QueryException {
    if(value.isEmpty()) return null;

    long min = value.itemAt(0).itr(info), max = value.itemAt(value.size() - 1).itr(info);
    // swap values if order is descending
    if(min > max) {
      final long t = max;
      max = min;
      min = t;
    }
    // range is small enough to be computed with long values
    if(max < 3037000500L) return Int.get((min + max) * (max - min + 1) / 2);
    // compute larger ranges
    final BigInteger bs = BigInteger.valueOf(min), be = BigInteger.valueOf(max);
    final BigInteger bi = bs.add(be).multiply(be.subtract(bs).add(BigInteger.ONE)).
        divide(BigInteger.valueOf(2));
    final long l = bi.longValue();
    // check if result is small enough to be represented as long value
    if(bi.equals(BigInteger.valueOf(l))) return Int.get(l);
    throw RANGE_X.get(info, bi);
  }

  /**
   * Multiply constant value.
   * @param seq singleton sequence
   * @return sum or {@code null}
   * @throws QueryException query exception
   */
  private Item multiply(final SingletonSeq seq) throws QueryException {
    Item it = seq.itemAt(0);
    if(it.type.isUntyped()) it = Dbl.get(it.dbl(info));
    return it.type.isNumber() ? Calc.MULT.ev(it, Int.get(seq.size()), info) : null;
  }
}
