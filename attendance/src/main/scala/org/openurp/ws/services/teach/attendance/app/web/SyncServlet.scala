/*
 * OpenURP, Open University Resouce Planning
 *
 * Copyright (c) 2013-2014, OpenURP Software.
 *
 * OpenURP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenURP is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openurp.ws.services.teach.attendance.app.web

import org.beangle.commons.lang.Dates
import org.beangle.commons.lang.time.Stopwatch
import org.beangle.commons.logging.Logging
import org.beangle.data.jdbc.query.JdbcExecutor
import org.openurp.ws.services.teach.attendance.app.impl.DeviceRegistry
import org.openurp.ws.services.teach.attendance.app.util.{JsonBuilder, Params, Render}
import org.openurp.ws.services.teach.attendance.app.util.Consts.{DeviceId, Rule}
import org.openurp.ws.services.teach.attendance.app.util.DateUtils.{toDateStr, toTimeStr}

import javax.servlet.{ServletRequest, ServletResponse}
import javax.servlet.http.HttpServlet

/**
 * 同步服务器心跳
 *
 * @author chaostone
 * @version 1.0, 2014/03/22
 * @since 0.0.1
 */
class SyncServlet extends HttpServlet with Logging {

  var executor: JdbcExecutor = _

  var deviceRegistry: DeviceRegistry = _

  override def service(req: ServletRequest, res: ServletResponse) {
    val watch = new Stopwatch(true)
    var retcode: Int = -1
    var devid = 0
    var retmsg, room = ""
    val params = Params.require(DeviceId).get(req, Rule)
    if (!params.ok) {
      retmsg = params.msg.values.mkString(";")
    } else {
      devid = params(DeviceId)
      deviceRegistry.get(devid) match {
        case Some(d) => {
          if (executor.update("update DEVICE_JS set qdsj=? where devid=?", Dates.now, devid) < 1) {
            deviceRegistry.unregister(devid)
            retmsg = "无法连接，没有对应的教室信息"
          } else {
            room = d.room.name
            retcode = 0
          }
        }
        case None =>
          retmsg = "无法连接，没有对应的教室信息"
      }
    }
    //FIXME rename classname to roomname
    val rs = new JsonBuilder
    rs.add("retcode", retcode).add("retmsg", retmsg);
    rs.add("classname", room).add("devid", devid).add("serverdate", toDateStr()).add("servertime", toTimeStr())
    Render.render(res, rs)
    logger.debug("app.synctime using {}", watch)
  }
}