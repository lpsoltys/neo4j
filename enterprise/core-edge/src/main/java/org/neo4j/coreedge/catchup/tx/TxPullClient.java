/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.catchup.tx;

import java.util.concurrent.CompletableFuture;

import org.neo4j.coreedge.catchup.CatchUpClient;
import org.neo4j.coreedge.catchup.CatchUpClientException;
import org.neo4j.coreedge.catchup.CatchUpResponseAdaptor;
import org.neo4j.coreedge.catchup.CatchupResult;
import org.neo4j.coreedge.discovery.NoKnownAddressesException;
import org.neo4j.coreedge.identity.MemberId;
import org.neo4j.coreedge.identity.StoreId;
import org.neo4j.kernel.monitoring.Monitors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TxPullClient
{
    private final CatchUpClient catchUpClient;
    private PullRequestMonitor pullRequestMonitor;

    public TxPullClient( CatchUpClient catchUpClient, Monitors monitors )
    {
        this.catchUpClient = catchUpClient;
        this.pullRequestMonitor = monitors.newMonitor( PullRequestMonitor.class );
    }

    public CatchupResult pullTransactions( MemberId from, StoreId storeId, long startTxId,
            TxPullResponseListener txPullResponseListener )
            throws CatchUpClientException, NoKnownAddressesException
    {
        pullRequestMonitor.txPullRequest( startTxId );
        return catchUpClient.makeBlockingRequest( from, new TxPullRequest( startTxId, storeId ), 30, SECONDS,
                new CatchUpResponseAdaptor<CatchupResult>()
                {
                    @Override
                    public void onTxPullResponse( CompletableFuture<CatchupResult> signal, TxPullResponse response )
                    {
                        txPullResponseListener.onTxReceived( response );
                    }

                    @Override
                    public void onTxStreamFinishedResponse( CompletableFuture<CatchupResult> signal, TxStreamFinishedResponse response )
                    {
                        signal.complete( response.status() );
                    }
                } );
    }
}
