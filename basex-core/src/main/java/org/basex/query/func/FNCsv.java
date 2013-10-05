package org.basex.query.func;

import static org.basex.data.DataText.*;
import static org.basex.io.serial.SerializerOptions.*;
import static org.basex.query.QueryText.*;
import static org.basex.query.util.Err.*;
import static org.basex.util.Token.*;

import java.io.*;

import org.basex.build.*;
import org.basex.io.serial.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.util.csv.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Functions for parsing CSV input.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public class FNCsv extends StandardFunc {
  /** Element: options. */
  private static final QNm Q_OPTIONS = QNm.get("options", CSVURI);
  /** The {@code header} key. */
  private static final byte[] HEADER = token("header");
  /** The {@code separator} key. */
  private static final byte[] SEPARATOR = token("separator");

  /**
   * Constructor.
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  public FNCsv(final InputInfo ii, final Function f, final Expr... e) {
    super(ii, f, e);
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    switch(sig) {
      case _CSV_PARSE:     return parse(ctx);
      case _CSV_SERIALIZE: return serialize(ctx);
      default:             return super.item(ctx, ii);
    }
  }

  /**
   * Converts CSV input to an element node.
   * @param ctx query context
   * @return element node
   * @throws QueryException query exception
   */
  private FElem parse(final QueryContext ctx) throws QueryException {
    final byte[] input = checkStr(expr[0], ctx);
    final Item opt = expr.length > 1 ? expr[1].item(ctx, info) : null;
    final TokenMap map = new FuncParams(Q_OPTIONS, info).parse(opt);

    try {
      return new CsvConverter(options(map)).convert(input);
    } catch(final IOException ex) {
      throw BXCS_PARSE.thrw(info, ex);
    }
  }

  /**
   * Serializes the specified XML document as CSV.
   * @param ctx query context
   * @return string representation
   * @throws QueryException query exception
   */
  private Str serialize(final QueryContext ctx) throws QueryException {
    final ANode node = checkNode(expr[0], ctx);
    final Item opt = expr.length > 1 ? expr[1].item(ctx, info) : null;
    final TokenMap map = new FuncParams(Q_OPTIONS, info).parse(opt);

    final SerializerOptions opts = new SerializerOptions();
    opts.set(S_METHOD, M_CSV);
    opts.set(S_CSV, options(map).toString());
    // serialize node and remove carriage returns
    return Str.get(delete(serialize(node.iter(), opts), '\r'));
  }

  /**
   * Creates CSV options.
   * @param map map
   * @return options
   */
  private CsvOptions options(final TokenMap map) {
    final CsvOptions copts = new CsvOptions();

    final byte[] header = map.get(HEADER);
    if(header != null) copts.set(CsvOptions.HEADER, Util.yes(string(header)));

    final byte[] sep = map.get(SEPARATOR);
    if(sep != null) copts.set(CsvOptions.SEPARATOR, string(sep));
    return copts;
  }
}
