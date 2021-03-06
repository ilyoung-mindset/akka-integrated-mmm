package me.mbcu.integrated.mmm.ops.common

import me.mbcu.integrated.mmm.ops.common.Side.Side
import me.mbcu.integrated.mmm.ops.common.Status.Status
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Side extends Enumeration {
  type Side = Value
  val buy, sell = Value

  implicit val read = Reads.enumNameReads(Side)
  implicit val write = Writes.enumNameWrites

  def reverse(a: Side): Side = if (a == Side.buy) sell else buy
  def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
}

object Status extends Enumeration {
  type Status = Value
  val active, filled, partiallyFilled, cancelled, expired, debug = Value

  implicit val read = Reads.enumNameReads(Status)
  implicit val write = Writes.enumNameWrites
}

object Offer {
  implicit val jsonFormat = Json.format[Offer]
  val sortTimeDesc: Seq[Offer] => Seq[Offer] = in => in.sortWith(_.createdAt > _.createdAt)
  val sortTimeAsc: Seq[Offer] => Seq[Offer] = in => in.sortWith(_.createdAt < _.createdAt)

  def newOffer(symbol: String, side: Side, price: BigDecimal, quantity: BigDecimal): Offer = Offer("unused", symbol, side, Status.debug, -1L, None, quantity, price, None)

  def splitToBuysSels(in: Seq[Offer]): (Seq[Offer], Seq[Offer]) = {
    val t = in.partition(_.side == Side.buy)
    (sortBuys(t._1), sortSels(t._2))
  }

  def sortBuys(buys: Seq[Offer]): scala.collection.immutable.Seq[Offer] =
    collection.immutable.Seq(buys.sortWith(_.price > _.price): _*)

  def sortSels(sels: Seq[Offer]): scala.collection.immutable.Seq[Offer] =
    collection.immutable.Seq(sels.sortWith(_.price < _.price): _*)

  def dump(bot: Bot, sortedBuys: Seq[Offer], sortedSels: Seq[Offer]): String = {
    val builder = StringBuilder.newBuilder
    builder.append(System.getProperty("line.separator"))
    builder.append(s"Open Orders ${bot.exchange}: ${bot.pair}")
    builder.append(System.getProperty("line.separator"))
    builder.append(s"sells : ${sortedSels.size}")
    builder.append(System.getProperty("line.separator"))
    sortedSels.sortWith(_.price > _.price).foreach(s => {
      builder.append(s"id:${s.id} quantity:${s.quantity.bigDecimal.toPlainString} price:${s.price.bigDecimal.toPlainString} filled:${s.cumQuantity.getOrElse(BigDecimal("0").bigDecimal.toPlainString)}")
      builder.append(System.getProperty("line.separator"))
    })
    builder.append(s"buys : ${sortedBuys.size}")
    builder.append(System.getProperty("line.separator"))
    sortedBuys.foreach(b => {
      builder.append(s"id:${b.id} quantity:${b.quantity.bigDecimal.toPlainString} price:${b.price.bigDecimal.toPlainString} filled:${b.cumQuantity.getOrElse(BigDecimal("0").bigDecimal.toPlainString)}")
      builder.append(System.getProperty("line.separator"))
    })

    builder.toString()
  }

  object Implicits {
    implicit val writes: Writes[Offer] = (o: Offer) => Json.obj(
      "id" -> o.id,
      "symbol" -> o.symbol,
      "side" -> o.side,
      "status" -> o.status,
      "createdAt" -> o.createdAt,
      "updatedAt" -> o.updatedAt,
      "quantity" -> o.quantity,
      "price" -> o.price,
      "cumQuantity" -> o.cumQuantity
    )

    implicit val reads: Reads[Offer] = (
      (JsPath \ "id").read[String] and
        (JsPath \ "symbol").read[String] and
        (JsPath \ "side").read[Side] and
        (JsPath \ "status").read[Status] and
        (JsPath \ "createdAt").read[Long] and
        (JsPath \ "updatedAt").readNullable[Long] and
        (JsPath \ "quantity").read[BigDecimal] and
        (JsPath \ "price").read[BigDecimal] and
        (JsPath \ "cumQuantity").readNullable[BigDecimal]
      ) (Offer.apply _)
  }

}

case class Offer(
  id: String,
  symbol: String,
  side: Side,
  status: Status,
  createdAt: Long,
  updatedAt: Option[Long],
  quantity: BigDecimal,
  price: BigDecimal,
  cumQuantity: Option[BigDecimal]
)
